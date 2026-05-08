package moe.styx.web.components

interface EditorState<T> {
    fun current(): T

    fun replace(newValue: T): T

    fun update(transform: (T) -> T): T {
        return replace(transform(current()))
    }
}

class LocalEditorState<T>(
    initialValue: T,
    private val onChange: (T) -> Unit = {}
) : EditorState<T> {
    private var value: T = initialValue

    override fun current(): T {
        return value
    }

    override fun replace(newValue: T): T {
        value = newValue
        onChange(newValue)
        return newValue
    }
}

private class DelegatingEditorState<T>(
    private val currentProvider: () -> T,
    private val replaceValue: (T) -> T
) : EditorState<T> {
    override fun current(): T {
        return currentProvider()
    }

    override fun replace(newValue: T): T {
        return replaceValue(newValue)
    }
}

fun <T> delegatingEditorState(current: () -> T, replace: (T) -> T): EditorState<T> {
    return DelegatingEditorState(current, replace)
}
