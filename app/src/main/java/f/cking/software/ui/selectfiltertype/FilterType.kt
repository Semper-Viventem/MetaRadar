package f.cking.software.ui.selectfiltertype

enum class FilterType(val typeName: String) {
    NAME("By  name"),
    ADDRESS("By address"),
    BY_FIRST_DETECTION("By first detection period"),
    BY_LAST_DETECTION("By last detection period"),
    BY_IS_FAVORITE("By is favorite"),
    BY_MANUFACTURER("By manufacturer"),
    BY_MIN_DETECTION_TIME("By min detection time"),
    BY_LOGIC_ANY("Logic OR filter"),
    BY_LOGIC_ALL("Logic AND filter"),
    BY_LOGIC_NOT("Logic NOT filter"),
}