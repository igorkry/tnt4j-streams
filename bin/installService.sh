#!/bin/bash

SERVICE_FILE=$(tempfile)

cp ./nix/tnt4j-streams-service.sh $SERVICE_FILE
chmod +x "$SERVICE_FILE"

echo "--- Customize ---"
echo "I'll now ask you some information to customize script"
echo "Press Ctrl+C anytime to abort."
echo "Empty values are not accepted."
echo ""

prompt_token() {
  local VAL=""
  while [ "$VAL" = "" ]; do
    echo -n "${2:-$1} : "
    read VAL
    if [ "$VAL" = "" ]; then
      echo "Please provide a value"
    fi
  done
  VAL=$(printf '%q' "$VAL")
  eval $1=$VAL
  sed -i "s|\($1\)=\(.*\)|\1=$(printf '%q' "$VAL")|" $SERVICE_FILE
}

prompt_token 'NAME'          '            Service name'
if [ -f "/etc/init.d/$NAME" ]; then
  echo "Error: service '$NAME' already exists"
  exit 1
fi

prompt_token 'DESC' 		 '             Description'
prompt_token 'FILE_PATH'     ' TNT4J-Streams root path'
prompt_token 'USERNAME'      '                    User'
prompt_token 'PARSER_CONFIG' '           Parser config'
if ! id -u "$USERNAME" &> /dev/null; then
  echo "Error: user '$USERNAME' not found"
  exit 1
fi

echo ""

echo "--- Installation ---"
if [ ! -w /etc/init.d ]; then
  echo "You don't gave me enough permissions to install service myself."
  echo "That's smart, always be really cautious with third-party shell scripts!"
  echo "You should now type those commands as superuser to install and run your service:"
  echo ""
  echo "   mv \"$SERVICE_FILE\" \"/etc/init.d/$NAME\""
  echo "   touch \"/var/log/$NAME.log\" && chown \"$USERNAME\" \"/var/log/$NAME.log\""
  echo "   update-rc.d \"$NAME\" defaults"
  echo "   service \"$NAME\" start"
else
  echo "1. mv \"$SERVICE_FILE\" \"/etc/init.d/$NAME\""
  mv -v "$SERVICE_FILE" "/etc/init.d/$NAME"
  echo "2. touch \"/var/log/$NAME.log\" && chown \"$USERNAME\" \"/var/log/$NAME.log\""
  touch "/var/log/$NAME.log" && chown "$USERNAME" "/var/log/$NAME.log"
  echo "3. update-rc.d \"$NAME\" defaults"
  update-rc.d "$NAME" defaults
  echo "4. service \"$NAME\" start"
  service "$NAME" start
fi


echo "update-rc.d -f \"$NAME\" remove && rm -f \"/etc/init.d/$NAME\"" > uninstallService.sh
chmod +x uninstallService.sh

