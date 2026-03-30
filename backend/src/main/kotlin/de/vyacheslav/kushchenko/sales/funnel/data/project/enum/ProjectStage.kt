package de.vyacheslav.kushchenko.sales.funnel.data.project.enum

enum class ProjectStage {
    QUALIFICATION,
    PROPOSAL,
    NEGOTIATION,
    INVOICE_ISSUED,
    PRODUCTION,
    WAITING_FOR_PAYMENT;

    fun isBackwardTo(target: ProjectStage): Boolean = ordinal > target.ordinal
}
