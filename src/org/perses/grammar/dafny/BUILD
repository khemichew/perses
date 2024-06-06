package(default_visibility = [
    "//visibility:public",
])

load("//src/org/perses/grammar:antlr_util.bzl", "antlr_codegen_combined")
load("@io_bazel_rules_kotlin//kotlin:jvm.bzl", "kt_jvm_library")
load("//antlropt/src/org/perses/antlr/pnf:pnf.bzl", "pnf_convert")

pnf_convert(
    name = "normalize_dafny_to_pnf",
    src = "dafny.g4",
    out = "PnfDafny.g4",
    start_rule_name = "translation_unit",
)

antlr_codegen_combined(
    name = "pnf_dafny_grammar",
    grammar_file = "PnfDafny.g4",
    java_pkg_name = "org.perses.grammar.dafny",
    lexer_java_file_name = "PnfDafnyLexer.java",
    parser_java_file_name = "PnfDafnyParser.java",
)

kt_jvm_library(
    name = "language_kind",
    srcs = ["LanguageDafny.kt"],
    deps = [
        "//src/org/perses/program:language_kind",
    ],
)

java_library(
    name = "parser_facade",
    srcs = ["DafnyParserFacade.java"],
    deps = [
        ":language_kind",
        ":pnf_dafny_grammar",
        "//:flogger",
        "//:guava",
        "//src/org/perses/antlr:parse_tree_with_parser",
        "//src/org/perses/grammar:grammar_facade",
        "//src/org/perses/program:language_kind",
        "@maven//:org_antlr_antlr4_runtime",
    ],
)