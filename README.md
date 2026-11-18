# SimpleKothNK

Plugin KOTH individual, ligero y configurable para **NukkitPetteriM1Edition (PM1E)**.

- Autor: **BrunoJaegerK**
- Java: **17**
- Build: **Maven**
- PlaceholderAPI: integración opcional con **PlaceholderAPI-nukkit** de Creeperface01.

## Características

- Región cúbica definida por dos esquinas, incluso en un área pequeña de un mundo existente.
- Evento individual: solo un jugador puede capturar.
- Si hay dos o más jugadores dentro de la zona, la captura queda disputada y el contador se pausa.
- Si el capturador sale de la región, su progreso se reinicia.
- BossBar nativa de PM1E con tiempo restante.
- Anuncio de inicio y victoria en el chat.
- Comandos configurables al ganar.
- Placeholders de PlaceholderAPI-nukkit.
- Sin scoreboard propio, clanes, economía ni sistemas de arenas.

## Comandos

| Comando | Descripción |
|---|---|
| `/koth` | Muestra la ayuda |
| `/koth set pos1` | Guarda la primera esquina en la posición actual |
| `/koth set pos2` | Guarda la segunda esquina en la posición actual |
| `/koth start` | Inicia el KOTH |
| `/koth stop` | Detiene el KOTH |
| `/koth status` | Muestra estado, tiempo y capturador |
| `/koth reload` | Recarga la configuración |

Permiso: `simplekoth.admin` (por defecto OP).

## Configuración

En `plugins/SimpleKothNK/config.yml`:

- `capture-time-seconds`: segundos que se necesitan para ganar.
- `winner-commands`: comandos ejecutados desde consola. Usa `{player}`.
- `messages`: mensajes configurables.
- `region`: se guarda automáticamente al usar `/koth set pos1` y `/koth set pos2`.

La región exige que ambas posiciones estén en el mismo mundo. No se modifica ningún bloque ni se protege el resto del mundo.

## Reglas de captura

1. Con el evento activo, un jugador solo dentro de la región empieza a capturar.
2. Si entra otro jugador, el contador se pausa mientras haya más de una persona en la región.
3. Cuando vuelve a quedar un solo jugador, el capturador que siga dentro continúa con su progreso.
4. Si el capturador sale o se desconecta, su progreso se reinicia. Si queda otro jugador solo dentro, empieza desde cero.
5. Al completar el tiempo, se anuncia el ganador y se ejecutan los comandos configurados.

Jugadores muertos no cuentan como participantes dentro de la zona.

## PlaceholderAPI-nukkit

Se integra con la API de [Creeperface01/PlaceholderAPI-nukkit](https://github.com/Creeperface01/PlaceholderAPI-nukkit). No es la PlaceholderAPI de Bukkit/Spigot.

| Placeholder | Resultado |
|---|---|
| `%koth_time%` | Tiempo restante `MM:SS` |
| `%koth_time_seconds%` | Segundos restantes |
| `%koth_status%` | `stopped`, `waiting` o `capturing` |
| `%koth_capturer%` | Nombre del capturador o vacío |

La integración se activa si PlaceholderAPI está instalado; no es una dependencia obligatoria.

## Compilar

Requisitos: JDK 17 y Maven 3.8.6 o superior.

```bash
mvn clean package
```

El JAR se genera en `target/SimpleKothNK-1.0.0-SNAPSHOT.jar`.

## Compatibilidad

El proyecto usa la API pública de Nukkit publicada en OpenCollab para compilar en CI y las llamadas específicas de BossBar verificadas en el código fuente de PM1E. La compatibilidad binaria exacta debe contrastarse con el JAR de la compilación PM1E que utilice el servidor de destino.
