package io.papermc.mache.constants

const val REPO_URL = "https://repo.papermc.io/repository/maven-releases/"

const val GRADLE_DIR = ".gradle"
const val MACHE_DIR = "mache"
const val JSONS_DIR = "$MACHE_DIR/jsons"

const val MC_MANIFEST = "$JSONS_DIR/McManifest.json"
const val MC_VERSION = "$JSONS_DIR/McVersion.json"

const val INPUT_DIR = "$MACHE_DIR/input"
const val DOWNLOAD_INPUT_JAR = "$INPUT_DIR/download_input.jar"
const val INPUT_JAR = "$INPUT_DIR/input.jar"
const val INPUT_MAPPINGS = "$INPUT_DIR/input_mappings.txt"
const val INPUT_LIBRARIES_LIST = "$INPUT_DIR/input_libraries.json"

const val REMAPPED_JAR = "$INPUT_DIR/remapped.jar"
const val DECOMP_JAR = "$INPUT_DIR/decomp.jar"
const val DECOMP_CFG = "$INPUT_DIR/decomp.cfg"

const val PATCHED_JAR = "$INPUT_DIR/patched.jar"
const val FAILED_PATCH_JAR = "$INPUT_DIR/failed_patch.jar"
