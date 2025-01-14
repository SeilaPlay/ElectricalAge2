package org.eln2.mc.common.cells.foundation.objects

/**
 * This represents an immutable set of simulation objects.
 * All possible simulation objects are stored in fields, because there won't be many (probably only electrical, thermal and mechanical),
 * so there is no reason not to skip an allocation for a map/array.
 * */
class SimulationObjectSet(objects: List<ISimulationObject>) {
    constructor(vararg objects: ISimulationObject) : this(objects.asList())

    private var electrical: ElectricalObject? = null

    private val mask: SimulationObjectMask

    init {
        if (objects.isEmpty()) {
            error("Tried to create empty simulation object set.")
        }

        var objectMask = SimulationObjectMask.EMPTY

        objects.forEach {
            when (it.type) {
                SimulationObjectType.Electrical -> {
                    if (electrical != null) {
                        error("Duplicate electrical object")
                    }

                    electrical = it as ElectricalObject
                }
            }

            objectMask += it.type
        }

        mask = objectMask
    }

    fun hasObject(type: SimulationObjectType): Boolean {
        return mask.hasFlag(type)
    }

    private fun getObject(type: SimulationObjectType): ISimulationObject {
        when (type) {
            SimulationObjectType.Electrical -> {
                if (electrical == null) {
                    error("Tried to get electrical component, which was null")
                }

                return electrical!!
            }
        }
    }

    val electricalObject get() = getObject(SimulationObjectType.Electrical) as ElectricalObject

    fun process(function: ((ISimulationObject) -> Unit)) {
        if (electrical != null) {
            function(electrical!!)
        }
    }
}
