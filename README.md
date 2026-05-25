# ⑂ ErnesGit-O

Tu herramienta personal de gestión Git. Interfaz gráfica desktop para las operaciones git más habituales y avanzadas.

---

## Requisitos

| Herramienta | Versión mínima |
|-------------|---------------|
| Java JDK    | 17+           |
| Maven       | 3.8+          |
| Git         | 2.x           |

## Instalación y ejecución

```bash
# 1. Clonar / descomprimir el proyecto
cd ernesgit-o

# 2. Compilar y ejecutar directamente
mvn javafx:run

# 3. O generar un JAR ejecutable
mvn package
java -jar target/ernesgit-o-1.0.0.jar
```

---

## Funcionalidades

### 🌿 Ramas
- Ver todas las ramas locales y remotas
- Filtrado en tiempo real
- **Checkout** con un doble clic o botón
- **Crear** rama (con checkout automático opcional)
- **Nueva rama desde un commit** específico
- **Merge** en la rama actual
- **Renombrar** rama
- **Eliminar** rama (con opción `-D` force)

### 📜 Historial
- Tabla de commits con hash, refs, mensaje, autor y fecha
- **Drop último commit** en tres modos:
  - `--soft` (mantiene cambios en staging)
  - `--mixed` (mantiene cambios sin staging)
  - `--hard` (descarta todo — ¡irreversible!)
- **Editar mensaje** del último commit (`--amend`)
- **Cherry-pick** desde el menú contextual (click derecho)
- **Revert** commit (crea un commit inverso)
- **Nueva rama** desde cualquier commit
- Copiar hash al portapapeles

### 📦 Stash
- Guardar cambios con mensaje descriptivo
- Opción `-u` para incluir ficheros no rastreados
- **Apply** (aplica y conserva el stash)
- **Pop** (aplica y elimina el stash)
- **Drop** (elimina sin aplicar)
- **Crear rama** desde un stash
- Preview del diff de cada stash

### 🔗 Remotos
- Ver todos los remotos con URLs fetch/push
- Añadir y eliminar remotos
- **Push** con opciones:
  - `-u` (set upstream tracking)
  - `--force-with-lease` (force seguro)
  - `--force` (force sin verificación)
- **Pull** con opción `--rebase`
- **Fetch** individual o `--all`
- **Sync Upstream**: fetch upstream → merge → push origin
  (ideal para sincronizar forks)

### ⚡ Conflictos
- Listado de ficheros en conflicto
- Abrir directamente en el editor del sistema
- Marcar ficheros como resueltos (`git add`)
- Continuar merge con mensaje personalizado
- Abortar merge

---

## Atajos

| Zona            | Acción               |
|-----------------|----------------------|
| Barra superior  | `Refrescar` → recarga el panel activo |
| Historial       | Click derecho sobre commit → menú contextual |
| Cualquier panel | `Abrir repo` → cambiar de repositorio |

---

## Personalización

- El tema de colores está en `src/main/resources/style.css`
- Basado en **Tokyo Night** — modifica las variables `-color-*` al inicio del fichero para cambiar toda la paleta

---

## Roadmap

- [ ] Graph visual de commits (árbol de ramas)
- [ ] Integración con GitHub/GitLab PRs
- [ ] Configuración de firma GPG
- [ ] Diff side-by-side
- [ ] Soporte para submodules

---

*ErnesGit-O v1.0.0 — hecho con ☕ y JavaFX 21*
