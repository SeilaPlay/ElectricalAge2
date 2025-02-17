package org.eln2.mc.common.content

import mcp.mobius.waila.api.IPluginConfig
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.phys.Vec3
import org.ageseries.libage.sim.electrical.mna.Circuit
import org.ageseries.libage.sim.electrical.mna.component.Resistor
import org.eln2.mc.client.render.PartialModels
import org.eln2.mc.client.render.foundation.BasicPartRenderer
import org.eln2.mc.common.cells.CellRegistry
import org.eln2.mc.common.cells.foundation.CellBase
import org.eln2.mc.common.cells.foundation.CellPos
import org.eln2.mc.common.cells.foundation.objects.ElectricalComponentInfo
import org.eln2.mc.common.cells.foundation.objects.ElectricalObject
import org.eln2.mc.common.cells.foundation.objects.SimulationObjectSet
import org.eln2.mc.common.parts.foundation.CellPart
import org.eln2.mc.common.parts.foundation.ConnectionMode
import org.eln2.mc.common.parts.foundation.IPartRenderer
import org.eln2.mc.common.parts.foundation.PartPlacementContext
import org.eln2.mc.common.space.RelativeRotationDirection
import org.eln2.mc.extensions.TooltipBuilderExtensions.resistor
import org.eln2.mc.integration.waila.IWailaProvider
import org.eln2.mc.integration.waila.TooltipBuilder

/**
 * The resistor object has a single resistor. At most, two connections can be made by this object.
 * */
class ResistorObject : ElectricalObject(), IWailaProvider {
    private lateinit var resistor: Resistor
    val hasResistor get() = this::resistor.isInitialized

    /**
     * Gets or sets the resistance.
     * */
    var resistance: Double = 1.0
        set(value) {
            field = value

            if(hasResistor){
                resistor.resistance = value
            }
        }

    override val maxConnections = 2

    override fun offerComponent(neighbour: ElectricalObject): ElectricalComponentInfo {
        return ElectricalComponentInfo(resistor, indexOf(neighbour))
    }

    override fun recreateComponents() {
        resistor = Resistor()
        resistor.resistance = resistance
    }

    override fun registerComponents(circuit: Circuit) {
        circuit.add(resistor)
    }

    override fun build() {
        connections.forEach { connectionInfo ->
            val remote = connectionInfo.obj
            val localInfo = offerComponent(remote)
            val remoteInfo = remote.offerComponent(this)

            localInfo.component.connect(localInfo.index, remoteInfo.component, remoteInfo.index)
        }
    }

    override fun appendBody(builder: TooltipBuilder, config: IPluginConfig?) {
        builder.resistor(resistor)
    }
}

class ResistorCell(pos: CellPos, id: ResourceLocation) : CellBase(pos, id) {
    override fun createObjectSet(): SimulationObjectSet {
        return SimulationObjectSet(ResistorObject())
    }
}

class ResistorPart(id: ResourceLocation, placementContext: PartPlacementContext) :
    CellPart(id, placementContext, CellRegistry.RESISTOR_CELL.get()) {

    override val baseSize = Vec3(1.0, 1.0, 1.0)

    override fun createRenderer(): IPartRenderer {
        return BasicPartRenderer(this, PartialModels.BATTERY)
    }

    override fun recordConnection(direction: RelativeRotationDirection, mode: ConnectionMode) {}

    override fun recordDeletedConnection(direction: RelativeRotationDirection) {}
}
