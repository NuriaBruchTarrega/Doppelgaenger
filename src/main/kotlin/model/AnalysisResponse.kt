package model

import utility.Clone


class AnalysisResponse(
    val clones: List<Clone>,
    val metrics: CloneMetrics
)
