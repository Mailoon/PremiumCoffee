#!/bin/sh
# ==========================================================================
# Entrypoint del frontend — decide entre desarrollo y produccion.
#
# El objetivo de este script es que la MISMA imagen sirva para los dos
# modos, y que la diferencia sea una variable de entorno y nada mas.
#
#   MODE=dev   -> arranca el servidor de desarrollo (nuxt dev), con recarga
#                 en caliente. Necesita el codigo fuente, que en el compose
#                 llega por volumen.
#   MODE=prod  -> arranca el build de produccion (node .output/server/index.mjs).
#                 Es lo que se despliega. No toca el codigo fuente.
#
# Sin MODE, o con cualquier valor raro, se arranca en produccion. El
# default tiene que ser el modo seguro: si la variable se pierde por el
# camino, lo que se levanta es la version optimizada, no el servidor de
# desarrollo con un watcher escribiendo en el filesystem.
#
# --------------------------------------------------------------------------
# POR QUE ESTE SCRIPT CAMBIA DE USUARIO
# --------------------------------------------------------------------------
#
# La imagen arranca como root, y cada modo baja al usuario que le toca:
#
#   dev  -> se queda como root.
#           Es un contenedor de desarrollo: escribe en volumenes que vienen
#           del host, y el host es de otro usuario. Necesita poder escribir
#           ahi sin pelearse con los permisos. Ademas el watcher de Vite
#           escribe en .nuxt, que es un volumen.
#
#   prod -> baja a "node" con su-exec.
#           Un contenedor que corre como root durante toda su vida es un
#           contenedor que un exploit puede usar para escalar. En produccion
#           no hace falta ningun privilegio: el proceso solo lee .output y
#           escucha en un puerto.
#
# El reparto es al reves de lo que suele hacerse, y a proposito: en
# desarrollo los permisos molestan mucho mas de lo que molesta el root, y
# en produccion el root no aporta nada.
#
# El "exec" es importante: replaces el shell con el proceso real, de modo que
# el proceso Node recibe directamente el SIGTERM y no un shell intermedio.
# Sin exec, la senal de parada se pierde en medio y el contenedor acaba
# matado a los 9 segundos en vez de apagarse limpiamente.
# ==========================================================================

set -e

if [ "${MODE}" = "dev" ]; then
  # Se llama al binario de Nuxt directamente y no a "npm run dev": npm
  # funciona como intermediario y se traga la senal de parada, que es justo
  # lo que se quiere evitar.
  exec node ./node_modules/nuxt/bin/nuxt.mjs dev
fi

# su-exec viene en la imagen oficial de Node para Alpine. Es un cambio de
# usuario de un solo proceso, sin grupos de procesos intermedios, asi que la
# senal llega directa a Node.
if [ "$(id -u)" = "0" ]; then
  exec su-exec node node .output/server/index.mjs
fi

exec node .output/server/index.mjs
