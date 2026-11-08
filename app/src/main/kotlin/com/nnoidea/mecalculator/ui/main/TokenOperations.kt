package com.nnoidea.mecalculator.ui.main

internal fun getListAtPath(root: List<MathToken>, path: List<CursorStep>): List<MathToken>? {
    var current = root
    for (step in path) {
        val tok = current.getOrNull(step.tokenIndex) ?: return null
        current = when (step.slot) {
            SlotType.FRAC_NUM -> (tok as? MathToken.FractionToken)?.num ?: return null
            SlotType.FRAC_DEN -> (tok as? MathToken.FractionToken)?.den ?: return null
            SlotType.POW_EXP -> (tok as? MathToken.PowerToken)?.exp ?: return null
            SlotType.SQRT_RAD -> (tok as? MathToken.SqrtToken)?.rad ?: return null
        }
    }
    return current
}

internal fun replaceListAtPath(
    root: List<MathToken>,
    path: List<CursorStep>,
    newSubList: List<MathToken>,
): List<MathToken> {
    if (path.isEmpty()) return newSubList
    val first = path.first()
    val rest = path.drop(1)
    val mut = root.toMutableList()
    val tok = mut.getOrNull(first.tokenIndex) ?: return root
    mut[first.tokenIndex] = when (first.slot) {
        SlotType.FRAC_NUM -> {
            val f = tok as? MathToken.FractionToken ?: return root
            f.copy(num = replaceListAtPath(f.num, rest, newSubList))
        }
        SlotType.FRAC_DEN -> {
            val f = tok as? MathToken.FractionToken ?: return root
            f.copy(den = replaceListAtPath(f.den, rest, newSubList))
        }
        SlotType.POW_EXP -> {
            val p = tok as? MathToken.PowerToken ?: return root
            p.copy(exp = replaceListAtPath(p.exp, rest, newSubList))
        }
        SlotType.SQRT_RAD -> {
            val s = tok as? MathToken.SqrtToken ?: return root
            s.copy(rad = replaceListAtPath(s.rad, rest, newSubList))
        }
    }
    return mut
}

internal fun insertTokensAt(
    tokens: List<MathToken>,
    cursor: CursorState,
    toInsert: List<MathToken>,
): Pair<List<MathToken>, CursorState> {
    val currentList = getListAtPath(tokens, cursor.path) ?: tokens
    val targetPath = if (getListAtPath(tokens, cursor.path) != null) cursor.path else emptyList()
    val safeOffset = cursor.offset.coerceIn(0, currentList.size)
    val mut = currentList.toMutableList()
    mut.addAll(safeOffset, toInsert)
    val newRoot = replaceListAtPath(tokens, targetPath, mut)
    return newRoot to CursorState(path = targetPath, offset = safeOffset + toInsert.size)
}

internal fun insertFractionAt(
    tokens: List<MathToken>,
    cursor: CursorState,
): Pair<List<MathToken>, CursorState> {
    val currentList = getListAtPath(tokens, cursor.path) ?: tokens
    val targetPath = if (getListAtPath(tokens, cursor.path) != null) cursor.path else emptyList()
    val safeOffset = cursor.offset.coerceIn(0, currentList.size)

    var startCapture = safeOffset
    while (startCapture > 0) {
        val prev = currentList[startCapture - 1]
        when {
            prev is MathToken.CharToken && (
                prev.ch.isDigit() || prev.ch == '.' || prev.ch == 'π' || prev.ch == 'e' || prev.ch == '%'
            ) -> {
                startCapture--
            }
            prev is MathToken.FractionToken || prev is MathToken.PowerToken || prev is MathToken.SqrtToken -> {
                startCapture--
            }
            prev is MathToken.CharToken && prev.ch == ')' -> {
                var depth = 0
                var k = startCapture - 1
                var matched = false
                while (k >= 0) {
                    val t = currentList[k]
                    if (t is MathToken.CharToken && t.ch == ')') {
                        depth++
                    } else if ((t is MathToken.CharToken && t.ch == '(') || t is MathToken.FuncToken) {
                        depth--
                        if (depth == 0) {
                            startCapture = k
                            matched = true
                            break
                        }
                    }
                    k--
                }
                if (!matched) break
            }
            else -> break
        }
    }

    val capturedTokens = currentList.subList(startCapture, safeOffset).toList()
    val mut = currentList.toMutableList()
    repeat(safeOffset - startCapture) {
        mut.removeAt(startCapture)
    }
    val frac = MathToken.FractionToken(
        num = capturedTokens,
        den = emptyList(),
    )
    mut.add(startCapture, frac)
    val newRoot = replaceListAtPath(tokens, targetPath, mut)
    val nextSlot = if (capturedTokens.isNotEmpty()) SlotType.FRAC_DEN else SlotType.FRAC_NUM
    return newRoot to CursorState(
        path = targetPath + CursorStep(tokenIndex = startCapture, slot = nextSlot),
        offset = 0,
    )
}

internal fun insertPowerAt(
    tokens: List<MathToken>,
    cursor: CursorState,
): Pair<List<MathToken>, CursorState> {
    val currentList = getListAtPath(tokens, cursor.path) ?: tokens
    val targetPath = if (getListAtPath(tokens, cursor.path) != null) cursor.path else emptyList()
    val insertAt = cursor.offset.coerceIn(0, currentList.size)
    if (insertAt == 0) return tokens to cursor
    val prev = currentList[insertAt - 1]
    val hasValidBase = when (prev) {
        is MathToken.CharToken -> prev.ch.isDigit() || prev.ch == '.' || prev.ch == ')' || prev.ch == 'π' || prev.ch == 'e'
        is MathToken.FractionToken, is MathToken.PowerToken, is MathToken.SqrtToken -> true
        is MathToken.FuncToken -> false
    }
    if (!hasValidBase) return tokens to cursor

    val mut = currentList.toMutableList()
    mut.add(insertAt, MathToken.PowerToken(exp = emptyList()))
    val newRoot = replaceListAtPath(tokens, targetPath, mut)
    return newRoot to CursorState(
        path = targetPath + CursorStep(tokenIndex = insertAt, slot = SlotType.POW_EXP),
        offset = 0,
    )
}

internal fun insertSqrtAt(
    tokens: List<MathToken>,
    cursor: CursorState,
): Pair<List<MathToken>, CursorState> {
    val currentList = getListAtPath(tokens, cursor.path) ?: tokens
    val targetPath = if (getListAtPath(tokens, cursor.path) != null) cursor.path else emptyList()
    val safeOffset = cursor.offset.coerceIn(0, currentList.size)

    val mut = currentList.toMutableList()
    mut.add(safeOffset, MathToken.SqrtToken(rad = emptyList()))
    val newRoot = replaceListAtPath(tokens, targetPath, mut)
    return newRoot to CursorState(
        path = targetPath + CursorStep(tokenIndex = safeOffset, slot = SlotType.SQRT_RAD),
        offset = 0,
    )
}

internal fun backspaceAt(
    tokens: List<MathToken>,
    cursor: CursorState,
): Pair<List<MathToken>, CursorState> {
    val currentList = getListAtPath(tokens, cursor.path)
        ?: return tokens to CursorState(path = emptyList(), offset = tokens.size)
    val safeOffset = cursor.offset.coerceIn(0, currentList.size)

    if (safeOffset > 0) {
        return when (val prev = currentList[safeOffset - 1]) {
            is MathToken.CharToken, is MathToken.FuncToken -> {
                val mut = currentList.toMutableList().apply { removeAt(safeOffset - 1) }
                val newRoot = replaceListAtPath(tokens, cursor.path, mut)
                newRoot to cursor.copy(offset = safeOffset - 1)
            }
            is MathToken.FractionToken -> {
                tokens to CursorState(
                    path = cursor.path + CursorStep(safeOffset - 1, SlotType.FRAC_DEN),
                    offset = prev.den.size,
                )
            }
            is MathToken.PowerToken -> {
                tokens to CursorState(
                    path = cursor.path + CursorStep(safeOffset - 1, SlotType.POW_EXP),
                    offset = prev.exp.size,
                )
            }
            is MathToken.SqrtToken -> {
                tokens to CursorState(
                    path = cursor.path + CursorStep(safeOffset - 1, SlotType.SQRT_RAD),
                    offset = prev.rad.size,
                )
            }
        }
    } else if (cursor.path.isNotEmpty()) {
        val lastStep = cursor.path.last()
        val parentPath = cursor.path.dropLast(1)
        val parentList = getListAtPath(tokens, parentPath) ?: return tokens to cursor
        val parentToken = parentList.getOrNull(lastStep.tokenIndex) ?: return tokens to cursor

        return when (lastStep.slot) {
            SlotType.FRAC_DEN -> {
                val frac = parentToken as? MathToken.FractionToken ?: return tokens to cursor
                if (frac.den.isEmpty()) {
                    val mut = parentList.toMutableList()
                    mut.removeAt(lastStep.tokenIndex)
                    mut.addAll(lastStep.tokenIndex, frac.num)
                    val newRoot = replaceListAtPath(tokens, parentPath, mut)
                    newRoot to CursorState(
                        path = parentPath,
                        offset = lastStep.tokenIndex + frac.num.size,
                    )
                } else {
                    tokens to CursorState(
                        path = parentPath + CursorStep(lastStep.tokenIndex, SlotType.FRAC_NUM),
                        offset = frac.num.size,
                    )
                }
            }
            SlotType.FRAC_NUM -> {
                val frac = parentToken as? MathToken.FractionToken ?: return tokens to cursor
                if (frac.num.isEmpty() && frac.den.isEmpty()) {
                    val mut = parentList.toMutableList().apply { removeAt(lastStep.tokenIndex) }
                    val newRoot = replaceListAtPath(tokens, parentPath, mut)
                    newRoot to CursorState(path = parentPath, offset = lastStep.tokenIndex)
                } else {
                    tokens to CursorState(path = parentPath, offset = lastStep.tokenIndex)
                }
            }
            SlotType.POW_EXP -> {
                val pow = parentToken as? MathToken.PowerToken ?: return tokens to cursor
                if (pow.exp.isEmpty()) {
                    val mut = parentList.toMutableList().apply { removeAt(lastStep.tokenIndex) }
                    val newRoot = replaceListAtPath(tokens, parentPath, mut)
                    newRoot to CursorState(path = parentPath, offset = lastStep.tokenIndex)
                } else {
                    tokens to CursorState(path = parentPath, offset = lastStep.tokenIndex)
                }
            }
            SlotType.SQRT_RAD -> {
                val sqrt = parentToken as? MathToken.SqrtToken ?: return tokens to cursor
                if (sqrt.rad.isEmpty()) {
                    val mut = parentList.toMutableList().apply { removeAt(lastStep.tokenIndex) }
                    val newRoot = replaceListAtPath(tokens, parentPath, mut)
                    newRoot to CursorState(path = parentPath, offset = lastStep.tokenIndex)
                } else {
                    tokens to CursorState(path = parentPath, offset = lastStep.tokenIndex)
                }
            }
        }
    } else if (tokens.isNotEmpty()) {
        // Fallback: If cursor was at offset 0 at root level but tokens exist,
        // treat backspace as deleting from the end of the root expression!
        val lastIdx = tokens.size - 1
        return when (val lastTok = tokens[lastIdx]) {
            is MathToken.CharToken, is MathToken.FuncToken -> {
                val mut = tokens.toMutableList().apply { removeAt(lastIdx) }
                mut to CursorState(path = emptyList(), offset = lastIdx)
            }
            is MathToken.FractionToken -> {
                tokens to CursorState(
                    path = listOf(CursorStep(lastIdx, SlotType.FRAC_DEN)),
                    offset = lastTok.den.size,
                )
            }
            is MathToken.PowerToken -> {
                tokens to CursorState(
                    path = listOf(CursorStep(lastIdx, SlotType.POW_EXP)),
                    offset = lastTok.exp.size,
                )
            }
            is MathToken.SqrtToken -> {
                tokens to CursorState(
                    path = listOf(CursorStep(lastIdx, SlotType.SQRT_RAD)),
                    offset = lastTok.rad.size,
                )
            }
        }
    }
    return tokens to cursor
}

internal fun moveCursorInTokens(
    tokens: List<MathToken>,
    cursor: CursorState,
    dir: String,
): Pair<CursorState, Boolean> {
    val currentList = getListAtPath(tokens, cursor.path) ?: tokens
    val currentPath = if (getListAtPath(tokens, cursor.path) != null) cursor.path else emptyList()
    val safeOffset = cursor.offset.coerceIn(0, currentList.size)

    return when (dir) {
        "UP" -> {
            for (depth in currentPath.indices.reversed()) {
                val step = currentPath[depth]
                if (step.slot == SlotType.FRAC_DEN) {
                    val parentPath = currentPath.take(depth)
                    val parentList = getListAtPath(tokens, parentPath) ?: break
                    val frac = parentList.getOrNull(step.tokenIndex) as? MathToken.FractionToken ?: break
                    val newPath = parentPath + CursorStep(step.tokenIndex, SlotType.FRAC_NUM)
                    val targetOffset = if (depth == currentPath.lastIndex) {
                        safeOffset.coerceAtMost(frac.num.size)
                    } else {
                        frac.num.size
                    }
                    return CursorState(path = newPath, offset = targetOffset) to true
                }
            }
            cursor to false
        }
        "DOWN" -> {
            for (depth in currentPath.indices.reversed()) {
                val step = currentPath[depth]
                if (step.slot == SlotType.FRAC_NUM) {
                    val parentPath = currentPath.take(depth)
                    val parentList = getListAtPath(tokens, parentPath) ?: break
                    val frac = parentList.getOrNull(step.tokenIndex) as? MathToken.FractionToken ?: break
                    val newPath = parentPath + CursorStep(step.tokenIndex, SlotType.FRAC_DEN)
                    val targetOffset = if (depth == currentPath.lastIndex) {
                        safeOffset.coerceAtMost(frac.den.size)
                    } else {
                        frac.den.size
                    }
                    return CursorState(path = newPath, offset = targetOffset) to true
                } else if (step.slot == SlotType.POW_EXP || step.slot == SlotType.SQRT_RAD) {
                    val parentPath = currentPath.take(depth)
                    val parentList = getListAtPath(tokens, parentPath) ?: break
                    return CursorState(
                        path = parentPath,
                        offset = (step.tokenIndex + 1).coerceAtMost(parentList.size),
                    ) to true
                }
            }
            cursor to false
        }
        "LEFT" -> {
            if (safeOffset > 0) {
                val prevIdx = safeOffset - 1
                val nextCursor = when (val prev = currentList[prevIdx]) {
                    is MathToken.FractionToken -> CursorState(
                        path = currentPath + CursorStep(prevIdx, SlotType.FRAC_DEN),
                        offset = prev.den.size,
                    )
                    is MathToken.PowerToken -> CursorState(
                        path = currentPath + CursorStep(prevIdx, SlotType.POW_EXP),
                        offset = prev.exp.size,
                    )
                    is MathToken.SqrtToken -> CursorState(
                        path = currentPath + CursorStep(prevIdx, SlotType.SQRT_RAD),
                        offset = prev.rad.size,
                    )
                    is MathToken.CharToken, is MathToken.FuncToken -> CursorState(
                        path = currentPath,
                        offset = prevIdx,
                    )
                }
                nextCursor to true
            } else if (currentPath.isNotEmpty()) {
                val lastStep = currentPath.last()
                val parentPath = currentPath.dropLast(1)
                if (lastStep.slot == SlotType.FRAC_DEN) {
                    val parentList = getListAtPath(tokens, parentPath)
                    val frac = parentList?.getOrNull(lastStep.tokenIndex) as? MathToken.FractionToken
                    if (frac != null) {
                        return CursorState(
                            path = parentPath + CursorStep(lastStep.tokenIndex, SlotType.FRAC_NUM),
                            offset = frac.num.size,
                        ) to true
                    }
                }
                if (lastStep.slot == SlotType.SQRT_RAD) {
                    return CursorState(
                        path = parentPath,
                        offset = lastStep.tokenIndex,
                    ) to true
                }
                CursorState(
                    path = parentPath,
                    offset = lastStep.tokenIndex,
                ) to true
            } else {
                cursor to false
            }
        }
        "RIGHT" -> {
            if (safeOffset < currentList.size) {
                val nextIdx = safeOffset
                val nextCursor = when (val next = currentList[nextIdx]) {
                    is MathToken.FractionToken -> CursorState(
                        path = currentPath + CursorStep(nextIdx, SlotType.FRAC_NUM),
                        offset = 0,
                    )
                    is MathToken.PowerToken -> CursorState(
                        path = currentPath + CursorStep(nextIdx, SlotType.POW_EXP),
                        offset = 0,
                    )
                    is MathToken.SqrtToken -> CursorState(
                        path = currentPath + CursorStep(nextIdx, SlotType.SQRT_RAD),
                        offset = 0,
                    )
                    is MathToken.CharToken, is MathToken.FuncToken -> CursorState(
                        path = currentPath,
                        offset = nextIdx + 1,
                    )
                }
                nextCursor to true
            } else if (currentPath.isNotEmpty()) {
                val lastStep = currentPath.last()
                val parentPath = currentPath.dropLast(1)
                if (lastStep.slot == SlotType.FRAC_NUM) {
                    return CursorState(
                        path = parentPath + CursorStep(lastStep.tokenIndex, SlotType.FRAC_DEN),
                        offset = 0,
                    ) to true
                }
                if (lastStep.slot == SlotType.SQRT_RAD) {
                    val parentList = getListAtPath(tokens, parentPath)
                    return CursorState(
                        path = parentPath,
                        offset = (lastStep.tokenIndex + 1).coerceAtMost(parentList?.size ?: 0),
                    ) to true
                }
                val parentList = getListAtPath(tokens, parentPath)
                CursorState(
                    path = parentPath,
                    offset = (lastStep.tokenIndex + 1).coerceAtMost(parentList?.size ?: 0),
                ) to true
            } else {
                cursor to false
            }
        }
        else -> cursor to false
    }
}
