# grvTags - Sistema Completo de Tags

Un plugin moderno y completo para Minecraft que permite a los jugadores usar tags personalizables organizados por categorías, con soporte para MySQL, PlaceholderAPI y un sistema de GUIs intuitivo.

## 📋 Características

- ✅ **Sistema de Tags Dinámico** - Tags completamente personalizables con colores y formatos
- ✅ **Categorías Organizadas** - Organiza tus tags en categorías para mejor navegación
- ✅ **Base de Datos MySQL** - Almacenamiento seguro y eficiente
- ✅ **GUIs Modernos** - Interfaces gráficas intuitivas y atractivas
- ✅ **PlaceholderAPI** - Soporte completo para placeholders
- ✅ **Sistema de Permisos** - Control granular de acceso
- ✅ **Sincronización YAML** - Carga automática desde archivos de configuración
- ✅ **Compatible 1.20.1+** - Optimizado para versiones modernas

## 🚀 Instalación

### Requisitos
- **Minecraft Server**: Spigot/Paper 1.20.1 o superior
- **Java**: JDK 17 o superior
- **MySQL**: Base de datos MySQL configurada
- **PlaceholderAPI** (Opcional): Para usar placeholders
- **Vault** (Opcional): Para futuras funciones de economía

### Pasos de Instalación

1. **Descargar el Plugin**
   ```
   Coloca grvTags.jar en la carpeta /plugins/ de tu servidor
   ```

2. **Configurar Base de Datos**
    - Edita `config.yml` con tus credenciales MySQL:
   ```yaml
   database:
     type: mysql
     host: localhost
     port: 3306
     database: tu_base_de_datos
     user: tu_usuario
     password: tu_contraseña
     ssl: false
   ```

3. **Iniciar el Servidor**
    - El plugin creará automáticamente las tablas necesarias
    - Se generarán los archivos `tags.yml` y `categories.yml`

4. **Verificar Instalación**
   ```
   /grvtags info
   ```

## 📝 Comandos

### Comandos para Jugadores

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/tag` | Abre el menú principal de tags | `grvtags.use` |
| `/tags` | Alias del comando `/tag` | `grvtags.use` |

### Comandos de Administración

| Comando | Descripción | Permiso |
|---------|-------------|---------|
| `/grvtags help` | Muestra la ayuda completa | `op` |
| `/grvtags info` | Información del plugin y estadísticas | `op` |
| `/grvtags reload` | Recarga todas las configuraciones | `op` |
| `/grvtags database` | Estado y prueba de conexión de BD | `op` |

#### Gestión de Tags
| Comando | Descripción |
|---------|-------------|
| `/grvtags create <nombre> <categoria>` | Crea un nuevo tag |
| `/grvtags give <jugador> <tag>` | Da un tag a un jugador |
| `/grvtags take <jugador> <tag>` | Quita un tag de un jugador |
| `/grvtags set <jugador> <tag\|none>` | Establece el tag activo de un jugador |
| `/grvtags check <jugador>` | Ver información y estadísticas de un jugador |

#### Gestión de Categorías
| Comando | Descripción |
|---------|-------------|
| `/grvtags createcategory <nombre>` | Crea una nueva categoría |
| `/category` | Abre el GUI de gestión de categorías |

#### Editores (En Desarrollo)
| Comando | Descripción |
|---------|-------------|
| `/grvtags editor category` | Editor de categorías |
| `/grvtags editor tag` | Editor de tags específicos |
| `/grvtags editor tags` | Vista general de todos los tags |

### Ejemplos de Comandos

```bash
# Crear un tag VIP en la categoría "ranks"
/grvtags create VIP ranks

# Dar el tag "VIP" al jugador "Steve"
/grvtags give Steve VIP

# Establecer el tag activo de un jugador
/grvtags set Steve VIP

# Quitar el tag activo (volver al default)
/grvtags set Steve none

# Ver información de un jugador
/grvtags check Steve

# Crear una nueva categoría
/grvtags createcategory premium
```

## 🏷️ Placeholders (PlaceholderAPI)

### Placeholders Disponibles

| Placeholder | Descripción | Ejemplo |
|-------------|-------------|---------|
| `%grvtags_tag%` | Tag actual del jugador | `[VIP]` |
| `%grvtags_tags%` | Número total de tags desbloqueados | `5` |
| `%grvtags_tags_<categoria>%` | Tags desbloqueados por categoría | `%grvtags_tags_default%` → `2` |

### Ejemplos de Uso

#### En Chat (con plugin de chat)
```yaml
format: '%grvtags_tag% %player% &8» &f%message%'
# Resultado: [VIP] Steve » Hola mundo!
```

#### En Tab List
```yaml
tablist-format: '%grvtags_tag% %player%'
# Resultado: [VIP] Steve
```

#### En Scoreboard
```yaml
scoreboard:
  title: 'Mi Servidor'
  lines:
    - 'Tag: %grvtags_tag%'
    - 'Tags: %grvtags_tags%/25'
    - 'VIP Tags: %grvtags_tags_vip%'
```

## ⚙️ Configuración

### config.yml
```yaml
database:
  type: mysql
  host: localhost
  port: 3306
  database: survivalcore
  user: root
  password: tu_contraseña
  ssl: false

messages: []  # Mensajes personalizados (futuro)
```

### tags.yml - Ejemplo de Tags
```yaml
tags:
  default:  # Tag por defecto
    tag: '&8[&7?&8]'
    permission: ''
    description: '&7Tag por defecto'
    category: 'default'
    order: 0
    displayname: '&7Tag por defecto'
    display-item: 'BARRIER'
    cost: 0

  vip:
    tag: '&8[&6&lVIP&8]'
    permission: 'grvtags.tag.vip'
    description: '&7Tag exclusivo VIP'
    category: 'ranks'
    order: 1
    displayname: '&6Tag VIP: %tag%'
    display-item: 'DIAMOND'
    cost: 1000

  hexsupport:  # Soporte para colores HEX (1.16+)
    tag: '&8[&#afe4a4&lH&#b1cdb1&le&#b3b6bd&lx&#b59fca&lC&#b688d7&lo&#b871e3&ll&#ba5af0&lo&#bc43fd&lr&8]'
    permission: 'grvtags.tag.hex'
    description: '&7Soporte para colores hexadecimales'
    category: 'special'
    order: 1
    displayname: '&7Tag Hex: %tag%'
    display-item: 'NAME_TAG'
    cost: 200
```

### categories.yml - Ejemplo de Categorías
```yaml
categories:
  default:
    title: '&8Default Tags (Página %page%)'
    material: 'NAME_TAG'
    id_display: '&7&lTags por Defecto'
    slot: 11
    lore:
      - '&8&m-----------------------------'
      - ''
      - '&7Tags por defecto: &7%tags_amount%'
      - ''
      - '&8&m-----------------------------'
    permission: 'grvtags.category.default'
    permission-see-category: false

  vip:
    title: '&8VIP Tags (Página %page%)'
    material: 'DIAMOND'
    id_display: '&6&lTags VIP'
    slot: 13
    lore:
      - '&8&m-----------------------------'
      - ''
      - '&6Tags VIP: &7%tags_amount%'
      - ''
      - '&8&m-----------------------------'
    permission: 'grvtags.category.vip'
    permission-see-category: false
```

## 🔐 Permisos

### Permisos Principales
| Permiso | Descripción | Por Defecto |
|---------|-------------|-------------|
| `grvtags.admin` | Acceso completo de administración | `op` |
| `grvtags.use` | Usar el comando `/tags` | `true` |

### Permisos de Tags (Auto-generados)
| Permiso | Descripción |
|---------|-------------|
| `grvtags.tag.<nombre>` | Permiso para usar un tag específico |
| `grvtags.category.<nombre>` | Permiso para acceder a una categoría |

### Ejemplos de Permisos
```yaml
# Dar acceso a un tag específico
- grvtags.tag.vip
- grvtags.tag.moderador
- grvtags.tag.admin

# Dar acceso a categorías
- grvtags.category.ranks
- grvtags.category.special
- grvtags.category.seasonal
```

## 📚 Sistema de Base de Datos

### Tablas Creadas Automáticamente

1. **grvtags_categories** - Almacena las categorías
2. **grvtags_tags** - Almacena los tags
3. **grvtags_player_data** - Datos de jugadores
4. **grvtags_unlocked_tags** - Tags desbloqueados por jugador

### Migración y Sincronización
- El plugin sincroniza automáticamente los tags desde `tags.yml` a MySQL
- Cualquier cambio en los archivos YAML se aplicará tras un reload
- Los datos de jugadores se mantienen seguros en la base de datos

## 🎨 Personalización Avanzada

### Colores Soportados
- **Códigos Estándar**: `&a`, `&b`, `&c`, etc.
- **Colores HEX** (1.16+): `&#ffffff`, `&#ff0000`, etc.
- **Gradientes** (con plugins compatibles)

### Variables en Tags
| Variable | Descripción |
|----------|-------------|
| `%tag%` | Se reemplaza por el tag actual |
| `%page%` | Número de página en GUIs |
| `%tags_amount%` | Cantidad de tags en una categoría |

## 🐛 Solución de Problemas

### Error de Conexión a Base de Datos
```bash
# Verificar conexión
/grvtags database

# Revisar logs en console para errores SQL
# Asegurarse de que las credenciales sean correctas
```

### Tags no se Muestran
```bash
# Recargar configuraciones
/grvtags reload

# Verificar permisos del jugador
# Comprobar que el tag esté en la categoría correcta
```

### PlaceholderAPI no Funciona
```bash
# Verificar que PlaceholderAPI esté instalado
/papi list

# Registrar manualmente si es necesario
/papi reload
```

## 📞 Soporte

### Información del Plugin
- **Versión**: 1.0
- **Autor**: Brocolitx
- **Compatibilidad**: Spigot/Paper 1.20.1+
- **Dependencias**: MySQL, PlaceholderAPI (Opcional), Vault (Opcional)

### Comandos de Diagnóstico
```bash
/grvtags info          # Información general
/grvtags database      # Estado de base de datos
/version grvTags       # Versión del plugin
```

### Logs Importantes
- Los logs del plugin aparecen en la consola con el prefijo `[grvTags]`
- Errores críticos se registran en `logs/latest.log`
- Los cambios de configuración se notifican en la consola

---

**¡Gracias por usar grvTags!** 🎉

Para más información o reportar bugs, contacta al desarrollador.