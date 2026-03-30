package de.vyacheslav.kushchenko.sales.funnel.data.project.enum

enum class ProjectEventType {
    CREATED,
    STAGE_CHANGED,
    STATUS_CHANGED,
    STAGE_STATUS_CHANGED,
    AMOUNT_CHANGED,
    GLOBAL_COMMENT_CHANGED,
    SOURCE_SET,
    RESTORED_FROM_PAUSE
}
