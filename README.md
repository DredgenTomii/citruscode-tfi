# CitrusCode — Marketplace B2B agroindustrial

Trabajo Integrador Final · Cátedra **Administración de Sistemas Informáticos**
UTN — Facultad Regional Tucumán · Comisión **4K1** · **Grupo 13**

CitrusCode es una empresa ficticia de software y logística agroindustrial (AgTech) creada
para el TFI. Este repositorio contiene el sitio institucional y un **sistema funcional real**
—no un maquetado— con autenticación, base de datos en la nube y roles de usuario.

---

## Ver el proyecto en vivo

| | Link |
|---|---|
| Sitio institucional | https://citruscodeinit.netlify.app/ |
| Sistema funcional | https://citruscode.netlify.app/ |

Para entrar al sistema hay que **crear una cuenta** desde la propia pantalla de login,
eligiendo uno de los tres roles.

---

## Los tres roles

| Rol | Qué puede hacer |
|---|---|
| **Mayorista** | Explorar el catálogo (búsqueda, filtros por categoría, productor y producto, orden por precio, stock o calificación), armar un pedido por volumen, **repetir un pedido anterior** con un clic, seguir su estado y el **recorrido del lote en un mapa**, **chatear con el productor** (por pedido o desde la sección **Chats**, sin necesidad de comprar), **pedir cotizaciones** por volumen y aceptarlas con un clic, **reservar cosecha futura** desde el calendario, **seguir productores y marcar favoritos** para recibir alertas de novedades y bajas de precio, cancelarlo, descargar comprobantes en PDF, ver sus facturas, **calificar al productor** de 1 a 5 estrellas y consultar la **evolución de precios**. |
| **Productor** | Publicar, editar y eliminar lotes (en kg o litros) con **foto propia o generada con IA**, recibir **avisos de stock bajo**, confirmar los pedidos que recibe, hacer avanzar la trazabilidad, responder mensajes y **chatear con cualquier mayorista** desde la sección **Chats**, **responder cotizaciones** con un precio especial, anunciar sus cosechas en el **calendario de cosecha**, ver sus ganancias, su **reputación** y la evolución de sus precios, y pedir software a medida. |
| **Administración** | Dashboard con KPIs del marketplace, tabla de todos los pedidos, padrón de usuarios, **bitácora de actividad** (auditoría de quién hizo qué y cuándo), **ranking de proveedores**, evolución de precios y tablero Kanban de proyectos de software a medida. Las tablas se exportan a CSV. |

### Funciones destacadas

- **Ranking de proveedores.** El mayorista califica al productor (1 a 5 estrellas y comentario)
  cuando el pedido llega a *Entregado*. El orden usa un promedio bayesiano: un productor con una
  sola calificación de 5 no supera a otro con muchas calificaciones altas.
- **Fotos de producto.** Al publicar, se genera automáticamente una foto con IA a partir del
  nombre (Pollinations, sin texto en la imagen). El productor puede pedir otra o subir una foto
  real, que se achica en el navegador (~100 KB) y se guarda en el propio lote.
- **Evolución de precios.** Gráfico por producto que combina los cambios de precio publicados
  y el precio de cada pedido realizado, con mínimo, máximo y variación.
- **Mensajes por pedido** entre comprador y productor, en tiempo real.
- **Chats directos** entre mayoristas y productores, sin pedido de por medio: lista de conversaciones
  con el último mensaje, aviso de mensajes sin leer en el menú y botón *Contactar* en cada producto
  del catálogo.
- **Cotizaciones desde el chat.** El mayorista pide precio por un volumen; el productor responde con un
  precio especial y, si el mayorista acepta, se genera el pedido con ese precio.
- **Calendario de cosecha.** Cada productor anuncia qué va a tener y en qué meses; el mayorista reserva
  con anticipación y la reserva llega al chat para acordar el precio.
- **Seguimiento y alertas.** El mayorista sigue productores y marca productos favoritos (♡); el sistema
  avisa cuando publican algo nuevo o baja un precio.
- **Bitácora de actividad.** Registro de auditoría con fecha del servidor, filtros y exportación a CSV,
  que nadie puede editar ni borrar.
- **Ubicación exacta en el mapa.** La dirección se convierte en coordenadas (OpenStreetMap Nominatim) y el
  recorrido del pedido va de la finca al depósito reales; si no se encuentra, se usa la capital provincial.
- **Fotos en Firebase Storage.** La foto propia del lote se sube a Storage y el lote guarda solo la URL;
  si Storage no está habilitado, se guarda como antes dentro del lote.
- **Mapa de recorrido** de cada lote (Leaflet + OpenStreetMap), con el avance según la
  trazabilidad.
- **Modo claro / oscuro** en el sitio y en el sistema, con la misma paleta de la marca.
- **App instalable (PWA)** en computadora y Android, con manifiesto y *service worker*.

El rol de administración está restringido: además del código que pide el formulario, las
reglas de Firestore validan que el correo esté en una lista de habilitados
(ver `adminsHabilitados()` en `backend/firestore.rules`). Aunque alguien lea el código
fuente de la página, si su correo no está en esa lista el alta se rechaza **del lado del
servidor**.

---

## Cómo está hecho

Todo el sistema está escrito en **HTML, CSS y JavaScript puro**, sin frameworks ni proceso
de compilación: cada página es un único archivo autocontenido que se abre directamente en
el navegador. La persistencia y la autenticación las provee Firebase, consumido desde el
cliente vía SDK.

| Capa | Tecnología |
|---|---|
| Front-end | HTML5, CSS3 (custom properties), JavaScript ES5/ES6 sin frameworks |
| Autenticación | Firebase Authentication (email + contraseña) |
| Base de datos | Cloud Firestore, con listeners en tiempo real (`onSnapshot`) |
| Seguridad | Reglas de Firestore por rol (ver `backend/firestore.rules`) |
| PDFs | jsPDF (comprobantes de pedido y de entrega, generados en el navegador) |
| Códigos QR | qrcodejs (QR real y escaneable en el módulo de trazabilidad) |
| Mapas | Leaflet + mosaicos de OpenStreetMap |
| Imágenes con IA | Pollinations (generación por URL, sin clave) |
| App instalable | Web App Manifest + Service Worker (`sistema/sw.js`) |
| Hosting | Netlify (deploy por arrastre de carpeta) |

### Decisiones de diseño que vale la pena mirar

- **Numeración secuencial de pedidos sin backend.** Los números (`CC-8841`, `CC-8842`, …)
  se generan con una transacción atómica de Firestore sobre un contador compartido, de modo
  que dos mayoristas que compran al mismo tiempo nunca reciben el mismo número.
- **Tiempo real.** Cuando un productor confirma un pedido, la pantalla del mayorista se
  actualiza sola: no hay que refrescar. Lo resuelven los listeners `onSnapshot`.
- **Seguridad del lado del servidor.** Los permisos no dependen de ocultar botones en la
  interfaz: las reglas de Firestore verifican el rol del usuario contra su propio documento
  antes de permitir cualquier lectura o escritura.
- **Borrado lógico.** Eliminar una publicación la marca como archivada en lugar de borrarla,
  para que los pedidos históricos que la referencian sigan mostrándose completos.
- **Unidades.** Los lotes se publican en kilogramos o litros y la unidad viaja con el pedido,
  de forma que los subproductos (jugo concentrado, aceite esencial) se miden correctamente.
- **Reglas que protegen el ranking.** Solo el mayorista que hizo el pedido puede calificarlo, y
  solo si ya fue entregado. El mayorista únicamente puede cancelar u ocultar sus pedidos (no
  cambiar su estado), así nadie puede marcarse un pedido como entregado para calificar sin comprar.
- **Mensajes privados.** Cada chat es una subcolección del pedido; las reglas solo dejan leer y
  escribir a las dos partes (y leer a la administración). Los mensajes no se editan ni se borran.
- **Chats directos seguros.** Hay una conversación por par mayorista–productor y su id es
  `mayoristaUid_productorUid`. Las reglas verifican el rol real de cada parte contra su perfil (no se
  puede abrir un chat entre dos mayoristas ni a nombre de otro), y después de creada solo cambian el
  resumen del último mensaje y las marcas de leído.
- **Probado en emulador.** Las reglas se verificaron con el emulador de Firestore y
  `@firebase/rules-unit-testing` (32 casos de la primera versión y 59 de los chats, cotizaciones,
  calendario y bitácora, entre permitidos y rechazados).

---

## Estructura del repositorio

```
.
├── index.html                 Sitio institucional (landing de la empresa)
├── sistema/
│   ├── index.html             Sistema funcional con login y roles
│   ├── manifest.webmanifest   Manifiesto de la app instalable
│   ├── sw.js                  Service worker (instalación y apertura sin conexión)
│   └── icon-*.png             Íconos de la app
├── backend/
│   ├── firestore.rules        Reglas de seguridad de la base de datos
│   └── storage.rules          Reglas de Firebase Storage (fotos de los lotes)
└── docs/
    ├── CitrusCode_TFI_Primer_Entregable.pdf / .docx
    ├── CitrusCode_TFI_Entregable_Completo.pdf / .docx
    ├── red-topologia-empresa.png
    ├── red-arquitectura-sistema.png
    └── logo-citruscode.png
```

Si se modifican las reglas, hay que publicarlas en la consola de Firebase
(Firestore → Reglas → Publicar): el archivo del repositorio no se aplica solo. Las de
`storage.rules` se publican en Storage → Rules (Storage tiene que estar habilitado en el proyecto;
si no lo está, el sistema sigue guardando las fotos dentro del lote).

---

## Cómo correrlo localmente

No hace falta instalar nada ni levantar un servidor: alcanza con abrir los archivos.

```bash
git clone <url-del-repo>
cd citruscode-tfi
```

Después abrí `index.html` (sitio institucional) o `sistema/index.html` (sistema) con doble
clic. El sistema se conecta al proyecto de Firebase ya configurado, así que funciona igual
que la versión publicada.

Si preferís servirlo por HTTP:

```bash
python3 -m http.server 8000
```

y entrá a `http://localhost:8000`.

---

## Contenido del TFI

El documento completo del trabajo está en [`docs/`](docs/) e incluye la identidad de la
empresa, el análisis de mercado y FODA, misión y visión, objetivos estratégicos,
organigrama, la problemática relevada del sector citrícola del NOA y la solución informática
propuesta. La versión ampliada suma la segunda etapa del sistema (chats, cotizaciones,
calendario de cosecha, alertas, bitácora), la administración y operación (publicación,
respaldos, contingencia, costos), las pruebas y los manuales de usuario por rol.

---

## Limitaciones conocidas

Las dejamos documentadas a propósito, porque forman parte del análisis del trabajo:

- El código de administración es visible en el código fuente del cliente. Lo asumimos y lo
  compensamos con una lista de correos habilitados validada en las reglas de Firestore, que
  es el control que realmente importa. La solución completa sería mover la verificación a
  Cloud Functions con *custom claims* de Firebase.
- La facturación electrónica está **simulada**: se genera un CAE ficticio en lugar de
  consumir la API real de AFIP.
- La trazabilidad avanza por acción manual del productor; en producción se dispararía al
  escanear el QR desde el celular en el empaque.
- El modo Offline-First descrito en el TFI está implementado solo en parte: la app se instala y
  abre sin conexión, pero los datos (pedidos, stock) necesitan internet.
- El generador gratuito de imágenes limita cuántas fotos nuevas crea por persona. Mientras una
  foto no está lista se muestra una ilustración, y el sistema la reintenta en segundo plano;
  una vez generada, queda en caché para todos.
- La bitácora se escribe desde el navegador: las reglas garantizan que nadie registre a nombre de otro,
  falsee su rol o la fecha, ni edite o borre registros, pero un usuario con conocimientos técnicos
  podría omitir registrar una acción propia. La solución completa son *triggers* de Cloud Functions.
- La geocodificación usa el servicio gratuito de OpenStreetMap, que pide no superar una consulta por
  segundo; con muchos usuarios convendría un proveedor pago o guardar las coordenadas en el alta.
- Firebase Storage requiere el plan Blaze en proyectos nuevos; mientras no esté habilitado, las fotos se
  guardan dentro del lote como antes.

---

## Equipo — Grupo 13

Santillán Giuliano · Recalde · Rallé · Roldán

---

*Proyecto académico. CitrusCode es una empresa ficticia; los productores, cooperativas y
precios que aparecen en el catálogo son datos de demostración.*
