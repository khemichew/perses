package org.perses.grammar.dafny

import com.google.common.collect.ImmutableList
import com.google.common.collect.ImmutableSet
import org.perses.program.EnumFormatControl
import org.perses.program.LanguageKind

object LanguageDafny : LanguageKind(
  name = "dafny",
  extensions = ImmutableSet.of("dfy"),
  defaultCodeFormatControl = EnumFormatControl.COMPACT_ORIG_FORMAT,
  origCodeFormatControl = EnumFormatControl.ORIG_FORMAT,
  defaultFormatterCommands = ImmutableList.of(),
  allowedCodeFormatControl = ImmutableSet.of(
    EnumFormatControl.COMPACT_ORIG_FORMAT,
    EnumFormatControl.ORIG_FORMAT
  )
)