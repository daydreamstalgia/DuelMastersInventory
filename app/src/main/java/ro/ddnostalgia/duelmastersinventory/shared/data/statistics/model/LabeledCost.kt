package ro.ddnostalgia.duelmastersinventory.shared.data.statistics.model

data class LabeledCost(
    val label: String = "",
    val inboundCost: Double = 0.0,
    val outboundCost: Double = 0.0,
    val cost: Double = 0.0,
)