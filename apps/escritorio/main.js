/* App de escritorio de CitrusCode (Windows).
   Abre el sistema publicado en Netlify en una ventana propia, así cada cambio
   que se sube al sitio llega a la app sin reinstalar nada. Los enlaces a otros
   sitios se abren en el navegador y las descargas (comprobantes PDF, CSV)
   preguntan dónde guardar, como en cualquier programa. */
const { app, BrowserWindow, Menu, shell, nativeTheme } = require('electron');
const path = require('path');

const SISTEMA = 'https://citruscode.netlify.app/';
const HOST = 'citruscode.netlify.app';

let ventana = null;

function esDelSistema(url){
  try { return new URL(url).host === HOST; } catch(_){ return false; }
}

function crearVentana(){
  ventana = new BrowserWindow({
    width: 1280,
    height: 820,
    minWidth: 380,
    minHeight: 560,
    title: 'CitrusCode',
    icon: path.join(__dirname, 'build', 'icon.png'),
    backgroundColor: nativeTheme.shouldUseDarkColors ? '#0F1F16' : '#FFFDF6',
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      contextIsolation: true,
      sandbox: true,
      spellcheck: true
    }
  });
  ventana.maximize();
  ventana.once('ready-to-show', function(){ ventana.show(); });

  const web = ventana.webContents;
  // El sistema reconoce esta marca y no muestra los botones de descarga dentro de la app.
  web.setUserAgent(web.getUserAgent() + ' CitrusCodeApp/' + app.getVersion() + ' (Windows)');

  web.setWindowOpenHandler(function(detalles){
    if(esDelSistema(detalles.url)) return { action: 'allow' };
    shell.openExternal(detalles.url);
    return { action: 'deny' };
  });
  web.on('will-navigate', function(e, url){
    if(esDelSistema(url) || url.startsWith('file:')) return;
    e.preventDefault();
    shell.openExternal(url);
  });
  web.on('did-fail-load', function(_e, codigo, _desc, url, principal){
    // -3 es una navegación cancelada (por ejemplo, al abrir un enlace afuera): no es un error.
    if(!principal || codigo === -3 || !esDelSistema(url)) return;
    ventana.loadFile(path.join(__dirname, 'sin-conexion.html'));
  });
  web.on('before-input-event', function(e, tecla){
    if(tecla.type !== 'keyDown') return;
    if(tecla.key === 'F5' || (tecla.control && tecla.key.toLowerCase() === 'r')){ web.reload(); e.preventDefault(); }
    if(tecla.alt && tecla.key === 'ArrowLeft' && web.canGoBack()){ web.goBack(); e.preventDefault(); }
    if(tecla.control && (tecla.key === '+' || tecla.key === '=')){ web.setZoomLevel(web.getZoomLevel() + 0.5); e.preventDefault(); }
    if(tecla.control && tecla.key === '-'){ web.setZoomLevel(web.getZoomLevel() - 0.5); e.preventDefault(); }
    if(tecla.control && tecla.key === '0'){ web.setZoomLevel(0); e.preventDefault(); }
  });

  ventana.on('closed', function(){ ventana = null; });
  ventana.loadURL(SISTEMA);
}

// Una sola ventana: si se abre el acceso directo de nuevo, se trae al frente la que ya está.
if(!app.requestSingleInstanceLock()){
  app.quit();
} else {
  app.on('second-instance', function(){
    if(!ventana) return;
    if(ventana.isMinimized()) ventana.restore();
    ventana.focus();
  });
  app.setAppUserModelId('ar.citruscode.escritorio');
  Menu.setApplicationMenu(null);
  app.whenReady().then(crearVentana);
  app.on('window-all-closed', function(){ app.quit(); });
}
