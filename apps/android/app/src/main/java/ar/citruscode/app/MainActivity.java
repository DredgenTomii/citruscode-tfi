package ar.citruscode.app;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.ContentValues;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Base64;
import android.webkit.CookieManager;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;

/**
 * App de Android de CitrusCode: abre el sistema publicado en Netlify dentro de
 * una ventana propia, con la sesión, las fotos y los comprobantes funcionando
 * igual que en el navegador. Así cada cambio que se sube a Netlify llega a la
 * app sin tener que publicar una versión nueva.
 */
public class MainActivity extends Activity {

    static final String SISTEMA = "https://citruscode.netlify.app/";
    private static final String HOST = "citruscode.netlify.app";
    private static final int ELEGIR_ARCHIVO = 1;

    private WebView web;
    private ValueCallback<Uri[]> archivoPendiente;

    /* Las descargas del sistema (PDF de comprobantes y CSV) son blobs generados
       en la página: el WebView no sabe guardarlos, así que se leen en JS y se
       pasan acá en base64. */
    private static final String JS_DESCARGAS =
        "(function(){" +
        " if(window.__ccDescargas) return; window.__ccDescargas = true;" +
        " function bajar(a){" +
        "  var url = a.href, nombre = a.download || 'CitrusCode';" +
        "  fetch(url).then(function(r){ return r.blob(); }).then(function(b){" +
        "   var fr = new FileReader();" +
        "   fr.onload = function(){ CitrusCodeApp.guardar(String(fr.result).split(',')[1], b.type || '', nombre); };" +
        "   fr.readAsDataURL(b);" +
        "  }).catch(function(){ CitrusCodeApp.aviso('No se pudo descargar el archivo'); });" +
        " }" +
        " function esDescarga(a){ return a && a.download && /^(blob|data):/.test(a.href || ''); }" +
        " var click = HTMLAnchorElement.prototype.click;" +
        " HTMLAnchorElement.prototype.click = function(){ if(esDescarga(this)) return bajar(this); return click.apply(this, arguments); };" +
        " var disparar = EventTarget.prototype.dispatchEvent;" +
        " EventTarget.prototype.dispatchEvent = function(e){" +
        "  if(e && e.type === 'click' && this instanceof HTMLAnchorElement && esDescarga(this)){ bajar(this); return false; }" +
        "  return disparar.apply(this, arguments);" +
        " };" +
        " document.addEventListener('click', function(e){" +
        "  var a = e.target && e.target.closest && e.target.closest('a[download]');" +
        "  if(esDescarga(a)){ e.preventDefault(); bajar(a); }" +
        " }, true);" +
        "})();";

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        web = new WebView(this);
        setContentView(web);

        WebSettings s = web.getSettings();
        s.setJavaScriptEnabled(true);
        s.setDomStorageEnabled(true);
        s.setDatabaseEnabled(true);
        s.setLoadWithOverviewMode(true);
        s.setUseWideViewPort(true);
        s.setMediaPlaybackRequiresUserGesture(false);
        s.setSupportMultipleWindows(false);
        // El sistema reconoce esta marca y no muestra los botones de descarga dentro de la app.
        s.setUserAgentString(s.getUserAgentString() + " CitrusCodeApp/" + BuildConfig.VERSION_NAME + " (Android)");
        CookieManager.getInstance().setAcceptCookie(true);
        CookieManager.getInstance().setAcceptThirdPartyCookies(web, true);

        web.addJavascriptInterface(new Puente(), "CitrusCodeApp");
        web.setWebViewClient(new Cliente());
        web.setWebChromeClient(new Cromo());
        web.setDownloadListener((url, ua, disposicion, mime, largo) -> {
            if (url.startsWith("http")) abrirAfuera(Uri.parse(url));
        });

        if (savedInstanceState != null) web.restoreState(savedInstanceState);
        else web.loadUrl(SISTEMA);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        web.saveState(outState);
    }

    @Override
    public void onBackPressed() {
        if (web.canGoBack()) web.goBack();
        else super.onBackPressed();
    }

    @Override
    protected void onPause() {
        super.onPause();
        CookieManager.getInstance().flush();
    }

    private void abrirAfuera(Uri uri) {
        try {
            startActivity(new Intent(Intent.ACTION_VIEW, uri));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No hay una app para abrir este enlace", Toast.LENGTH_SHORT).show();
        }
    }

    private class Cliente extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView v, WebResourceRequest req) {
            Uri uri = req.getUrl();
            String esquema = uri.getScheme() == null ? "" : uri.getScheme();
            if ((esquema.equals("https") || esquema.equals("http")) && HOST.equals(uri.getHost())) return false;
            if (esquema.equals("file")) return false;
            // Sitio institucional, WhatsApp, mapas, correo: se abren con la app que corresponda.
            abrirAfuera(uri);
            return true;
        }

        @Override
        public void onPageStarted(WebView v, String url, Bitmap icono) {
            v.evaluateJavascript(JS_DESCARGAS, null);
        }

        @Override
        public void onPageFinished(WebView v, String url) {
            v.evaluateJavascript(JS_DESCARGAS, null);
        }

        @Override
        public void onReceivedError(WebView v, WebResourceRequest req, WebResourceError err) {
            if (req.isForMainFrame()) v.loadUrl("file:///android_asset/sin-conexion.html");
        }
    }

    private class Cromo extends WebChromeClient {
        @Override
        public boolean onShowFileChooser(WebView v, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (archivoPendiente != null) archivoPendiente.onReceiveValue(null);
            archivoPendiente = callback;
            try {
                startActivityForResult(params.createIntent(), ELEGIR_ARCHIVO);
            } catch (ActivityNotFoundException e) {
                archivoPendiente = null;
                return false;
            }
            return true;
        }
    }

    @Override
    protected void onActivityResult(int pedido, int resultado, Intent datos) {
        if (pedido == ELEGIR_ARCHIVO && archivoPendiente != null) {
            archivoPendiente.onReceiveValue(WebChromeClient.FileChooserParams.parseResult(resultado, datos));
            archivoPendiente = null;
            return;
        }
        super.onActivityResult(pedido, resultado, datos);
    }

    /** Lo que la página puede pedirle a la app. */
    private class Puente {
        @JavascriptInterface
        public void guardar(String base64, String mime, String nombre) {
            String limpio = nombre.replaceAll("[\\\\/:*?\"<>|]", "_");
            String tipo = mime.isEmpty() ? "application/octet-stream" : mime.split(";")[0];
            try {
                byte[] datos = Base64.decode(base64, Base64.DEFAULT);
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    ContentValues cv = new ContentValues();
                    cv.put(MediaStore.Downloads.DISPLAY_NAME, limpio);
                    cv.put(MediaStore.Downloads.MIME_TYPE, tipo);
                    Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, cv);
                    if (uri == null) throw new Exception("sin destino");
                    try (OutputStream out = getContentResolver().openOutputStream(uri)) {
                        out.write(datos);
                    }
                    avisoYAbrir(limpio, uri, tipo);
                } else {
                    File carpeta = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    carpeta.mkdirs();
                    File f = new File(carpeta, limpio);
                    try (FileOutputStream out = new FileOutputStream(f)) {
                        out.write(datos);
                    }
                    runOnUiThread(() -> Toast.makeText(MainActivity.this, "Guardado en Descargas: " + limpio, Toast.LENGTH_LONG).show());
                }
            } catch (Exception e) {
                aviso("No se pudo guardar " + limpio);
            }
        }

        @JavascriptInterface
        public void aviso(String texto) {
            runOnUiThread(() -> Toast.makeText(MainActivity.this, texto, Toast.LENGTH_SHORT).show());
        }

        @JavascriptInterface
        public void reintentar() {
            runOnUiThread(() -> web.loadUrl(SISTEMA));
        }
    }

    private void avisoYAbrir(String nombre, Uri uri, String tipo) {
        runOnUiThread(() -> {
            Toast.makeText(this, "Guardado en Descargas: " + nombre, Toast.LENGTH_LONG).show();
            Intent ver = new Intent(Intent.ACTION_VIEW);
            ver.setDataAndType(uri, tipo);
            ver.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            try {
                startActivity(ver);
            } catch (ActivityNotFoundException ignorado) {
                // Sin visor instalado: el archivo igual quedó en Descargas.
            }
        });
    }
}
