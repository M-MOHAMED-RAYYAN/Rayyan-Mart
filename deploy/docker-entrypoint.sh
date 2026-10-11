#!/usr/bin/env bash
#
# RayyanMart container entrypoint.
#
# Tomcat's HTTP connector is hardcoded to 8080 in the stock server.xml; the
# Spring `server.port` property is ignored for a WAR running under an external
# Tomcat. Render requires the service to listen on $PORT (default 10000), so we
# patch server.xml to bind Tomcat's HTTP/1.1 connector to that value before
# launching Tomcat.

set -euo pipefail

PORT="${PORT:-8080}"

SERVER_XML="$CATALINA_HOME/conf/server.xml"
# Patch only the HTTP/1.1 connector (the stock file keeps AJP on 8009 / shutdown 8005).
sed -i "s|<Connector port=\"8080\"|<Connector port=\"${PORT}\"|" "$SERVER_XML"

echo "RayyanMart: starting Tomcat on 0.0.0.0:${PORT}"
exec catalina.sh run