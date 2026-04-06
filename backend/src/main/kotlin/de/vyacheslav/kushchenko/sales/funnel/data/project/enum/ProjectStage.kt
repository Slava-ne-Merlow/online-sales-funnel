package de.vyacheslav.kushchenko.sales.funnel.data.project.enum

enum class ProjectStage {
    QUALIFICATION,
    PROPOSAL,
    CONTRACTED,
    INVOICE_ISSUED,
    WAITING_FOR_PAYMENT;

    fun isBackwardTo(target: ProjectStage): Boolean = ordinal > target.ordinal
}
