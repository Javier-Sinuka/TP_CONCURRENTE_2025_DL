# clipetri

[![en](https://img.shields.io/badge/lang-en-red.svg)](../README.md)

## Introducción
clipetri es un framework de simulación concurrente y configurable escrito en Java. Proporciona un entorno robusto para modelar, simular y analizar sistemas con procesos concurrentes y paralelos utilizando Redes de Petri.

El proyecto enfatiza una clara separación de responsabilidades, con un motor de Red de Petri central, una capa de sincronización basada en un monitor para manejar la concurrencia y un sistema de políticas intercambiables para definir la lógica de disparo de transiciones ante un conflicto. Toda la simulación, incluida la estructura de la red, la temporización y el modelo de hilos, se define a través de archivos de configuración JSON externos. Además, cuenta con un conjunto de herramientas de análisis estático para calcular e informar sobre las propiedades estructurales de la red.

## Características
- **Configuración JSON**: Configura dinámicamente toda la Red de Petri, incluida su estructura (plazas, transiciones, matriz de incidencia), estado inicial, temporización de transiciones y modelo de hilos desde un único archivo JSON.
- **Herramientas de Análisis**: Un analizador estático incorporado calcula las propiedades estructurales clave de la red de Petri, que incluyen:
    - **P-Invariantes (Invariantes de Plaza)**: Identifica conjuntos de plazas donde la suma ponderada de tokens permanece constante, crucial para verificar las propiedades de conservación.
    - **T-Invariantes (Invariantes de Transición)**: Encuentra secuencias de disparos de transiciones que restauran la red a un estado anterior, útil para detectar ciclos y verificar la vivacidad.
    - **Conflictos Estructurales**: Detecta qué transiciones compiten por los mismos recursos de entrada (plazas).
- **Interfaz de Línea de Comandos Flexible**: Ejecuta simulaciones, análisis o ambos usando simples flags. Admite la ejecución de múltiples simulaciones y la generación de informes estadísticos agregados.
- **Simulación Multihilo**: Aprovecha un sofisticado modelo de hilos utilizando "Segmentos" para asignar grupos de transiciones a hilos de trabajo dedicados, lo que permite una verdadera simulación paralela.
- **Sincronización Basada en Monitores**: Garantiza transiciones de estado seguras para hilos utilizando un patrón de monitor clásico, que gestiona el acceso concurrente a la Red de Petri y coordina los hilos de trabajo a través de colas de condición.
- **Políticas de Disparo Intercambiables**: Cambie fácilmente entre diferentes estrategias para seleccionar qué transición disparar cuando varias están habilitadas. El proyecto incluye `RandomPolicy` y `PriorityPolicy`.
- **Transiciones Temporizadas**: Modele Redes de Petri temporizadas especificando retardos de disparo mínimos y máximos para cada transición.
- **Objetivos de Simulación Basados en Invariantes**: Defina una condición de finalización clara para las simulaciones estableciendo un `invariantLimit` en el archivo de configuración.
- **Loggeo Completo**: Genera archivos de log detallados tanto para las secuencias de transición como para la información de depuración.

## Instalación y Compilación

### Prerrequisitos
- **Java Development Kit (JDK)**: Versión 8 o superior.
- **Apache Maven**: El proyecto utiliza el Maven Wrapper, por lo que no se requiere una instalación local.

### Pasos
1.  **Clonar el repositorio:**
    ```bash
    git clone https://github.com/Javier-Sinuka/TP_CONCURRENTE_2025_DL.git
    ```
2.  **Navegar al directorio del proyecto:**
    ```bash
    cd TP_CONCURRENTE_2025_DL
    ```
3.  **Construir el proyecto usando el Maven Wrapper:**
    Este comando compilará el código fuente, ejecutará las pruebas y empaquetará la aplicación en un archivo JAR ejecutable ubicado en el directorio `target/`.

    *   En **Linux** o **macOS**:
        ```bash
        ./mvnw clean install
        ```
    *   En **Windows** (Command Prompt o PowerShell):
        ```bash
        .\mvnw.cmd clean install
        ```
    Un mensaje de `BUILD SUCCESS` indica que el simulador está listo.

## Uso
El simulador se ejecuta desde la línea de comandos y se controla con flags. Por defecto, ejecuta tanto un análisis estático como una única simulación utilizando `config_default.json` si no se especifica ningún archivo de configuración.

### Sintaxis Básica
```bash
java -jar target/clipetri.jar [opciones] [ruta_del_archivo_de_configuración]
```

### Opciones de Línea de Comandos

| Opción            | Descripción                                                                                                   |
|-------------------|---------------------------------------------------------------------------------------------------------------|
| `--analysis`      | Ejecutar solo el análisis estático de la red de Petri (invariantes P/T, conflictos).                           |
| `--simulation`    | Ejecutar solo la simulación.                                                                                  |
| `--runs <N>`      | Ejecutar la simulación `N` veces. Por defecto es 1.                                                           |
| `--statistics`    | Cuando `--runs > 1`, suprime los informes de ejecución individuales y muestra un informe estadístico final con promedios. |
| `--debug`         | Habilitar el log de depuración detallado. El archivo de log se especifica mediante `logPath` en la configuración JSON. |
| `--regex-checker` | Después de cada ejecución de la simulación, ejecutar `invariant_checker/invariant_checker.py`.                    |
| `--help`          | Mostrar el mensaje de ayuda y salir.                                                                          |

### Ejemplos
*   **Ejecutar tanto el análisis como una única simulación (comportamiento predeterminado):**
    ```bash
    java -jar target/clipetri.jar
    ```

*   **Ejecutar solo el análisis usando una configuración específica:**
    ```bash
    java -jar target/clipetri.jar --analysis simulation_configs/config_tp_2024.json
    ```

*   **Ejecutar 10 simulaciones con informes estadísticos y registro de depuración:**
    ```bash
    java -jar target/clipetri.jar --simulation --runs 10 --statistics --debug
    ```

## Archivo de Configuración
El comportamiento de la simulación está completamente controlado por un archivo JSON. A continuación se muestra un desglose de su estructura.

```json
{
  // Ruta para el archivo de log de depuración.
  "logPath": "log_default.txt",

  // La simulación se detendrá después de que se hayan completado esta cantidad de T-invariantes.
  "invariantLimit": 10,

  // Un array que representa el número inicial de tokens en cada plaza (P0, P1, ...).
  "initialMarking": [1, 0, 0, 0],

  // La matriz de incidencia (I). Las filas son plazas, las columnas son transiciones.
  // -1: El token se consume.
  //  1: El token se produce.
  //  0: No hay conexión.
  "incidence": [
    [-1, 0, 0, 1],
    [1, -1, 0, 0],
    [0, 1, -1, 0],
    [0, 0, 1, -1]
  ],

  // Rangos de tiempo [min, max] en milisegundos para cada transición.
  // Una transición solo puede dispararse si el tiempo desde que se habilitó por última vez cae dentro de este rango.
  // [0, 0] representa una transición inmediata.
  "timeRanges": [
    [100, 100],
    [100, 100],
    [100, 100],
    [100, 100]
  ],

  // Define el modelo de hilos. Cada objeto es un "Segmento".
  "segments": [
    {
      "name": "Default-Segment",
      "threadQuantity": 1,
      "transitions": [0, 1, 2, 3]
    }
  ],

// Política para elegir qué transición disparar cuando varias están habilitadas.
// Opciones: "random", "priority", "priority-probabilistic".
"policy": "random",

// Un mapa de pesos de transición utilizado por la política de "prioridad". Números más altos significan mayor prioridad.
// Requerido solo si la política es "priority".
"transitionWeights": {
  "0": 1,
  "1": 1,
  "2": 1,
  "3": 1
},

// Mapa de probabilidades (en enteros de 0 a 100) usado por "priority-probabilistic".
// Los valores de las transiciones en conflicto estructural deben sumar 100.
"transitionProbabilities": {
  "0": 25,
  "1": 25,
  "2": 25,
  "3": 25
}
}
```

## Contribuciones
Consulta el archivo [`CONTRIBUTING.md`](../CONTRIBUTING.md) para obtener pautas detalladas sobre el flujo de trabajo de desarrollo, los estándares de código y el modelo de branching.

### Estilo de Código
El proyecto se adhiere a la **Guía de Estilo de Java de Google**. Se utiliza el `spotless-maven-plugin` para verificar y formatear automáticamente el código. Para obtener más información, consulte las [Guía de Contribución](../CONTRIBUTING.md).

## Contacto
Para cualquier consulta o soporte, abre un issue en el repositorio de GitHub.
