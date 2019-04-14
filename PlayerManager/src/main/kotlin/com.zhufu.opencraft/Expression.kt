package com.zhufu.opencraft

import com.zhufu.opencraft.TextUtil.StringDetectResult
import com.zhufu.opencraft.Base.Extend.isDigit
import org.bukkit.Bukkit

class Expression(val expr: String) {
    val isNameSpecial: Boolean
        get() = expr.startsWith('\"')
    val playerName: String?
        get() {
            return if (expr.startsWith("\"")) {
                val end = expr.indexOf('\"', 1)
                if (end == -1) null
                else {
                    return expr.substring(1, end)
                }
            } else {
                expr.substring(0, expr.indexOf('.').also { if (it == -1) return null })
            }
        }

    private fun isOperatorMark(mark: String) = mark == "=" || mark == "+=" || mark == "-=" || mark == "+" || mark == "-"
    val nameEnding: Int
        get() {
            return expr.indexOf('.', (playerName?.length ?: return -1) + (if (isNameSpecial) 2 else 0) - 1)
        }
    val operatorMark: String?
        get() {
            var begin = nameEnding
            if (begin == -1)
                return null
            var seeMark = false
            var end = -1
            for (i in nameEnding until expr.length) {
                if (isOperatorMark(expr[i].toString())) {
                    seeMark = true
                    begin = i
                } else if (seeMark) {
                    end = i
                    break
                }
            }
            if (end == -1)
                return null
            return expr.substring(begin, end)
        }
    val variableName: String?
        get() {
            val begin = nameEnding
            if (begin == -1)
                return null
            var end = expr.length
            for (i in begin + 1 until expr.length) {
                if (isOperatorMark(expr[i].toString()) || expr[i] == ' ') {
                    end = i
                    break
                }
            }
            return expr.substring(begin+1, end)
        }
    val valueName: String?
        get() {
            val begin = nameEnding
            if (begin == -1)
                return null
            var beginning = -1
            var end = expr.length
            var seeMark = false
            for (i in begin until expr.length) {
                if (isOperatorMark(expr[i].toString())) {
                    seeMark = true
                } else if (seeMark) {
                    if (beginning == -1 && expr[i] != ' ') {
                        beginning = i
                    } else if (beginning != -1 && expr[i] == ' ') {
                        end = i
                        break
                    }
                }
            }
            if (beginning == -1)
                return null
            return expr.substring(beginning, end)
        }
    val value: Any?
        get() {
            val vName = valueName?:return null
            if (vName.isDigit()){
                return when (TextUtil.detectString(vName)){
                    StringDetectResult.String -> null
                    StringDetectResult.Int -> vName.toInt()
                    StringDetectResult.Long -> vName.toLong()
                    StringDetectResult.Double -> vName.toDouble()
                }
            }
            if (vName.startsWith('\"') && vName.endsWith('\"')){
                return buildString {
                    this.append(vName)
                    deleteCharAt(0)
                    deleteCharAt(lastIndex)
                }
            }
            if (vName.contains('(') && vName.contains(')')){
                fun buildClass(t: String): Any?{
                    val className = t.substring(0,vName.indexOf('('))
                    val args = t.substring(vName.indexOf('(')+1,vName.indexOfLast { it == ')' }).split(',')
                    val clazz = try { Class.forName(className) } catch (e: Exception){ return null }
                    clazz.constructors.forEach {
                        if (it.parameters.all { arg -> args.contains(arg.name) }){
                            val arguments = ArrayList<Any?>()
                            args.forEach { str ->
                                arguments.add(buildClass(str))
                            }
                            return it.newInstance(arguments)
                        }
                    }
                    return null
                }
                return buildClass(vName)
            }
            return null
        }

    fun check(): String? {
        if (expr.first() == '.') {
            return "语法错误：首字符期待字母或数字，却得到\".\""
        }
        if (!expr.contains('.')) {
            return "语法错误: 表达式必须包括\".\""
        }
        val vName = variableName
        if (vName.isNullOrEmpty()){
            return "语法错误: 找不到变量名"
        }
        val operator = operatorMark
        val value = valueName
        if (!operator.isNullOrEmpty() && value.isNullOrEmpty()){
            return "语法错误: 在操作符\"$operator\"后必须包含值"
        }
        if (value != null && !value.isDigit() && !value.startsWith('\"')){
            if (!value.contains('(') || !value.contains(')')){
                return "语法错误: 值名必须是数字、字符串或类名"
            } else if (this.value == null) {
                return "语法错误: 指定类无法被初始化"
            }
        }
        return null
    }

    fun apply(): String?{
        val instance = PlayerManager.findOfflinePlayer(Bukkit.getOfflinePlayer(playerName?:return null).uniqueId)
                ?: return TextUtil.error("玩家不存在")
        val vName = variableName
        val clazz = instance.javaClass
        val variable = clazz.methods.firstOrNull { it.name.equals("get$vName",ignoreCase = true) }?:return TextUtil.error("找不到变量: $vName")
        val operator = operatorMark
        if (operator != null){
            when (operator){
                "=" -> {
                    val setVar = clazz.methods.firstOrNull { it.name.equals("set$vName",ignoreCase = true) }?:return TextUtil.error("变量${vName}只读")
                    valueName?:return TextUtil.error("找不到值")
                    try {
                        setVar.invoke(instance, value)
                        instance.saveTag()
                    }catch (e: Exception){
                        return "${e::class.simpleName}: ${e.message}"
                    }
                }
            }
        } else {
            return variable.invoke(instance)?.toString()
        }
        return null
    }
}