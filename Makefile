LOGISIM331_JAR_FILE = c:\logisim\logisim-evolution-3.3.1.jar
LOGISIM404_JAR_FILE = c:\logisim\logisim-evolution-4.0.4hc.jar
LOGISIM400_JAR_FILE = c:\logisim\logisim-evolution-4.0.0hc.jar
LOGISIM271_JAR_FILE = ..\logisim-generic-2.7.1.jar
LOGISIM504_JAR_FILE = c:\logisim\logisim-evolution-5.0.4hc.jar
LOGISIM505_JAR_FILE = c:\logisim\logisim-evolution-5.0.5hc.jar
LOGISIM390_JAR_FILE = c:\logisim\logisim-evolution-3.9.0-all.jar
MANIFEST_FILE = MANIFEST.MF
BIN_DIR = .\bin

# GPU2.7.1 also works with Logisim Evolution 2.13.22,
#   Logisim Evolution 2.13.20, and Logisim Evolution
#   3.1.0 (Holy Cross version)

GPU2.7.1: classes2.7.1
	jar cmf $(MANIFEST_FILE) GPU.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

all: GPU2.7.1 GPU3.9.0 GPU5.0.4HC GPU4.0.4HC GPU3.3.1HC GPU4.0.0HC GPU5.0.5HC gpuelem

# GPU3.9.0 also works with Logisim Evolution 4.0.0

GPU3.9.0: classes3.9.0
	jar cmf $(MANIFEST_FILE) GPUev.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

GPU5.0.4HC: classes5.0.4hc
	jar cmf $(MANIFEST_FILE) GPUhc.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

GPU5.0.5HC: classes5.0.5hc
	jar cmf $(MANIFEST_FILE) GPUhc2.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

GPU4.0.4HC: classes4.0.4hc
	jar cmf $(MANIFEST_FILE) GPUhc0.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

GPU3.3.1HC: classes3.3.1hc
	jar cmf $(MANIFEST_FILE) GPUev0.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

# GPU4.0.0HC also works with Logisim Evolution 4.0.1 (Holy Cross)

GPU4.0.0HC: classes4.0.0hc
	jar cmf $(MANIFEST_FILE) GPUhc00.jar -C $(BIN_DIR) . src README.md Makefile Makefile.unix MANIFEST.MF doc resources

classes2.7.1:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM271_JAR_FILE) src\L2.7.1\GPU.java src\MyGPUlib.java

classes3.9.0:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM390_JAR_FILE) src\LE3.9.0\GPU.java src\MyGPUlib.java

classes5.0.4hc:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM504_JAR_FILE) src\LE5.0.4HC\GPU.java src\MyGPUlib.java

classes5.0.5hc:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM505_JAR_FILE) src\LE5.0.5HC\GPU.java src\MyGPUlib.java

classes4.0.4hc:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM404_JAR_FILE) src\LE4.0.4HC\GPU.java src\MyGPUlib.java

classes4.0.0hc:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM400_JAR_FILE) src\LE4.0.4HC\GPU.java src\MyGPUlib.java

classes3.3.1hc:
	javac -encoding "ISO-8859-1" -nowarn -d $(BIN_DIR) -classpath $(LOGISIM331_JAR_FILE) src\LE3.9.0\GPU.java src\MyGPUlib.java

gpuelem:
	cl gpuelem.c
