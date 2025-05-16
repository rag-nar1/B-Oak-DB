USERNAME=$(whoami)
USERDIR="/home/$USERNAME"
JavaFormater="$USERDIR/google-java-format-1.27.0-all-deps.jar"
java -jar $JavaFormater -i src/*/* 