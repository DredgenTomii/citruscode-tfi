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
| **Mayorista** | Explorar el catálogo, armar un pedido por volumen, seguir su estado, cancelarlo, descargar comprobantes en PDF y ver sus facturas. |
| **Productor** | Publicar y eliminar lotes (en kg o litros), confirmar los pedidos que recibe y hacer avanzar la trazabilidad de cada lote. |
| **Administración** | Dashboard con KPIs del marketplace, tabla de todos los pedidos y tablero Kanban de proyectos de software a medida. |

El rol de administración requiere un código de alta que no se publica en este README.

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

---

## Estructura del repositorio

```
.
├── index.html                 Sitio institucional (landing de la empresa)
├── sistema/
│   └── index.html             Sistema funcional con login y roles
├── backend/
│   └── firestore.rules        Reglas de seguridad de la base de datos
└── docs/
    ├── CitrusCode_TFI_Primer_Entregable.pdf
    └── logo-citruscode.png
```

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

- El código de alta de administración se valida en el cliente. En un sistema productivo esa
  verificación tendría que ocurrir del lado del servidor (por ejemplo, con Cloud Functions y
  *custom claims* de Firebase).
- La facturación electrónica está **simulada**: se genera un CAE ficticio en lugar de
  consumir la API real de AFIP.
- La trazabilidad avanza por acción manual del productor; en producción se dispararía al
  escanear el QR desde el celular en el empaque.
- El modo Offline-First descrito en el TFI está planteado en el diseño pero no implementado
  en este prototipo.

---

## Equipo — Grupo 13

Santillán Giuliano · Recalde · Rallé · Roldán

---

*Proyecto académico. CitrusCode es una empresa ficticia; los productores, cooperativas y
precios que aparecen en el catálogo son datos de demostración.*
