# SimpleKothNK

<p align="center">
  <img src="https://img.shields.io/badge/Minecraft-Bedrock%20Edition-43A047?style=for-the-badge&logo=minecraft&logoColor=white" alt="Minecraft Bedrock Edition">
  <img src="https://img.shields.io/badge/Server-NukkitPetteriM1Edition-2196F3?style=for-the-badge" alt="NukkitPetteriM1Edition">
  <img src="https://img.shields.io/badge/Java-17-orange?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 17">
  <img src="https://img.shields.io/badge/Build-Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven">
</p>

<p align="center"><b>Plugin KOTH por temporizador global para NukkitPetteriM1Edition, con regiones configurables, BossBar regresiva, efectos de zona y recompensas por comandos.</b></p>

---

Plugin KOTH ligero y configurable para **NukkitPetteriM1Edition (PM1E)**.

- Autor: **BrunoJaegerK**
- Java: **17**
- Build: **Maven**
- PlaceholderAPI: integración opcional con **PlaceholderAPI-nukkit** de Creeperface01.

## Características

- Región cúbica definida por dos esquinas, incluso en un área pequeña de un mundo existente.
- Temporizador global: avanza aunque la región esté vacía o disputada.
- Si hay dos o más jugadores dentro de la zona cuando el tiempo llega a cero, se elige un ganador aleatoriamente entre ellos.
- BossBar nativa de PM1E persistente, con tiempo restante y progreso; sigue la posición del jugador para evitar que desaparezca.
- Partículas de brillo alrededor de la región cuando no está disputada y partículas de enfado cuando hay varios jugadores dentro.
- Cuenta regresiva configurable antes de iniciar, con anuncios en los segundos elegidos.
- Inicio automático periódico configurable (por defecto cada 24 horas), con próxima fecha persistida tras reinicios.
- Mensajes independientes y editables en `lang.yml`.
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
| `/koth start` | Programa el inicio con cuenta regresiva |
| `/koth stop` | Detiene el KOTH |
| `/koth status` | Muestra estado, tiempo y capturador |
| `/koth reload` | Recarga la configuración |

Permiso: `simplekoth.admin` (por defecto OP).

## Configuración

En `plugins/SimpleKothNK/config.yml`:

- `capture-time-seconds`: segundos que se necesitan para ganar.
- `countdown.seconds`: duración de la cuenta regresiva; se fuerza un mínimo de 30 segundos.
- `countdown.announcements`: segundos restantes en los que se anuncia la cuenta regresiva (por defecto 30 y 20).
- `auto-start.enabled`: activa o desactiva los inicios automáticos.
- `auto-start.interval-seconds`: intervalo entre inicios automáticos. Usa `86400` para 24 horas o `259200` para 3 días.
- `auto-start.next-start-at`: fecha interna persistida automáticamente; no modificar manualmente.
- `winner-commands`: comandos ejecutados desde consola. Usa `{player}`.
- `region`: se guarda automáticamente al usar `/koth set pos1` y `/koth set pos2`.

Los mensajes están en `plugins/SimpleKothNK/lang.yml`; puedes personalizar prefijo, cuenta regresiva, inicio, victoria y respuestas de comandos.

La región exige que ambas posiciones estén en el mismo mundo. No se modifica ningún bloque ni se protege el resto del mundo.

## Reglas de captura

1. Al ejecutar `/koth start`, comienza una cuenta regresiva de al menos 30 segundos. Al terminar, se anuncia el inicio y comienza el evento.
2. El temporizador global avanza sin pausas, incluso si no hay jugadores dentro o la región está disputada.
3. La BossBar se muestra a todos los jugadores conectados y refleja el estado de la región.
4. Si hay varios jugadores dentro cuando el tiempo llega a cero, el ganador se elige aleatoriamente entre los jugadores vivos que estén dentro.
5. Si el tiempo llega a cero con la región vacía, permanece en cero y gana el primer jugador (o uno aleatorio entre varios) que entre.
6. Mientras haya disputa se muestran partículas de enfado; no se envían mensajes de captura o disputa al chat.
7. Al ganar, se anuncia al jugador y se ejecutan los comandos configurados.

El inicio automático está activado por defecto y programa el primer evento 24 horas después de la primera carga. La fecha del siguiente ciclo se conserva en `config.yml`; reiniciar el servidor no vuelve a contar las 24 horas desde cero.

Jugadores muertos no cuentan como participantes dentro de la zona.

## PlaceholderAPI-nukkit

Se integra con la API de [Creeperface01/PlaceholderAPI-nukkit](https://github.com/Creeperface01/PlaceholderAPI-nukkit). No es la PlaceholderAPI de Bukkit/Spigot.

| Placeholder | Resultado |
|---|---|
| `%koth_time%` | Tiempo restante `MM:SS` |
| `%koth_time_seconds%` | Segundos restantes |
| `%koth_status%` | `stopped`, `waiting`, `capturing` o `contested` |
| `%koth_capturer%` | Nombre del único jugador que esté dentro; vacío si hay cero o varios |

La integración se activa si PlaceholderAPI está instalado; no es una dependencia obligatoria.

## Compilar

Requisitos: JDK 17 y Maven 3.8.6 o superior.

```bash
mvn clean package
```

El JAR se genera en `target/SimpleKothNK-1.0.0-SNAPSHOT.jar`.

## Compatibilidad

El proyecto usa la API pública de Nukkit publicada en OpenCollab para compilar en CI y las llamadas específicas de BossBar verificadas en el código fuente de PM1E. La compatibilidad binaria exacta debe contrastarse con el JAR de la compilación PM1E que utilice el servidor de destino.

## ℹ️ About

Plugin KOTH ligero para NukkitPetteriM1Edition, con regiones configurables, captura disputada, BossBar y recompensas por comandos.

- **Autor:** BrunoJaegerK
- **Servidor objetivo:** NukkitPetteriM1Edition (Minecraft Bedrock)
- **Repositorio:** https://github.com/BrunoJaegerK/SimpleKothNK

## 📜 Licencia

Este proyecto se distribuye bajo **GPL-3.0**. Puedes utilizarlo, modificarlo y redistribuirlo respetando los términos de la licencia, conservando los avisos de copyright y atribución aplicables. En el caso de GPL-3.0, las versiones modificadas distribuidas deben permanecer bajo GPL-3.0 y proporcionar el código fuente correspondiente.

Consulta el archivo [LICENSE](LICENSE) para leer el texto completo.
