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
| **Mayorista** | Explorar el catálogo (búsqueda, filtros por categoría, productor y producto, orden por precio, stock o calificación), armar un pedido por volumen, **repetir un pedido anterior** con un clic, seguir su estado y el **recorrido del lote en un mapa**, **chatear con el productor**, cancelarlo, descargar comprobantes en PDF, ver sus facturas, **calificar al productor** de 1 a 5 estrellas y consultar la **evolución de precios**. |
| **Productor** | Publicar, editar y eliminar lotes (en kg o litros) con **foto propia o generada con IA**, recibir **avisos de stock bajo**, confirmar los pedidos que recibe, hacer avanzar la trazabilidad, responder mensajes, ver sus ganancias, su **reputación** y la evolución de sus precios, y pedir software a medida. |
| **Administración** | Dashboard con KPIs del marketplace, tabla de todos los pedidos, padrón de usuarios, **ranking de proveedores**, evolución de precios y tablero Kanban de proyectos de software a medida. Las tablas se exportan a CSV. |

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
- **Probado en emulador.** Las reglas se verificaron con el emulador de Firestore y
  `@firebase/rules-unit-testing` (32 casos entre permitidos y rechazados).

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
│   └── firestore.rules        Reglas de seguridad de la base de datos
└── docs/
    ├── CitrusCode_TFI_Primer_Entregable.pdf / .docx
    ├── CitrusCode_TFI_Entregable_Completo.pdf / .docx
    ├── red-topologia-empresa.png
    ├── red-arquitectura-sistema.png
    └── logo-citruscode.png
```

Si se modifican las reglas, hay que publicarlas en la consola de Firebase
(Firestore → Reglas → Publicar): el archivo del repositorio no se aplica solo.

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
propuesta.

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
- Las fotos subidas por los productores se guardan dentro del documento del lote. Con muchos
  productos convendría moverlas a Firebase Storage.
- El mapa ubica origen y destino por provincia (capital provincial), no por dirección exacta.

---

## Equipo — Grupo 13

Santillán Giuliano · Recalde · Rallé · Roldán

---

*Proyecto académico. CitrusCode es una empresa ficticia; los productores, cooperativas y
precios que aparecen en el catálogo son datos de demostración.*
