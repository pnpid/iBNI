package pnp.id.ibni;

import java.io.ByteArrayOutputStream;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.Toast;

public class MainActivity extends Activity {

	WebView wv;
	ProgressBar pb;
	WebView analytics;
	SharedPreferences keyValues;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_main);

		final LinearLayout loadingIndicator = (LinearLayout) findViewById(R.id.loadingIndicator);
		wv = (WebView) findViewById(R.id.webView1);
		wv.getSettings().setJavaScriptEnabled(true);
		wv.getSettings().setSupportMultipleWindows(true);
		wv.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		wv.getSettings().setAppCacheEnabled(false);
		pb = (ProgressBar) findViewById(R.id.progressBar2);
		pb.setMax(100);
		final ProgressBar fpb = pb;
		final WebView fwv = wv;
		wv.setWebChromeClient(new WebChromeClient() {
			@Override
			public void onProgressChanged(WebView view, int newProgress) {
				fpb.setProgress(newProgress);
				super.onProgressChanged(view, newProgress);
			}

		});
		wv.setWebViewClient(new WebViewClient() {

			@Override
			public void onPageFinished(WebView view, String url) {
				loadingIndicator.setVisibility(View.GONE);
				fwv.setVisibility(View.VISIBLE);
				fwv.requestFocus(View.FOCUS_DOWN);
				super.onPageFinished(view, url);
			}

			@Override
			public void onReceivedError(WebView view, int errorCode,
					String description, String failingUrl) {
				Toast.makeText(MainActivity.this,"Koneksi internet sedang labil",Toast.LENGTH_LONG).show();
			}

		});
		wv.loadUrl(getResources().getString(R.string.ibankUrl));

		analytics = new WebView(this);
		analytics.getSettings().setJavaScriptEnabled(true);
		analytics.setWebViewClient(new WebViewClient() {

		});
		int version = 0;

		try {
			PackageManager manager = getPackageManager();
			PackageInfo info = manager.getPackageInfo(getPackageName(), 0);
			version = info.versionCode;
		} catch (Exception e) {
		}
		analytics.loadUrl(getResources().getString(R.string.logUrl)
				+ "?path=LiveActivity?v=" + version);

		final int fversion = version;
		keyValues = getSharedPreferences("app_config", Context.MODE_PRIVATE);
		SharedPreferences.Editor keyValuesEditor = keyValues.edit();
		final SharedPreferences.Editor fkeyValuesEditor = keyValuesEditor;

		WebView adsEndWeb = (WebView) findViewById(R.id.adsEndWeb);
		Button adsEndClose = (Button) findViewById(R.id.adsEndClose);

		adsEndWeb.getSettings().setJavaScriptEnabled(true);
		adsEndWeb.getSettings().setSupportMultipleWindows(true);
		adsEndWeb.setWebChromeClient(new WebChromeClient() {
			@Override
			public boolean onCreateWindow(WebView view, boolean dialog,
					boolean userGesture, android.os.Message resultMsg) {
				WebView.HitTestResult result = view.getHitTestResult();
				String data = result.getExtra();
				Context context = view.getContext();
				Intent browserIntent = new Intent(Intent.ACTION_VIEW, Uri
						.parse(data));
				context.startActivity(browserIntent);
				return false;
			}
		});

		adsEndWeb.setWebViewClient(new WebViewClient() {
		});

		adsEndClose.setOnClickListener(new View.OnClickListener() {

			@Override
			public void onClick(View arg0) {
				MainActivity.this.closeAppProcess();
				MainActivity.this.finish();
			}
		});

		final WebView fadsEndWeb = adsEndWeb;
		new Thread() {

			@Override
			public void run() {
				try {
					HttpClient httpclient = new DefaultHttpClient();
					HttpResponse response = httpclient.execute(new HttpGet(
							getResources().getString(R.string.configUrl)
									+ "?v=" + fversion));
					StatusLine statusLine = response.getStatusLine();

					if (statusLine.getStatusCode() == HttpStatus.SC_OK) {
						ByteArrayOutputStream out = new ByteArrayOutputStream();
						response.getEntity().writeTo(out);
						out.close();

						final String responseString = out.toString();
						String[] configs = responseString.split(";");
						String ini_show_adsEnd = configs[2];

						if (ini_show_adsEnd != null
								&& ini_show_adsEnd.equals("ini_show_adsEnd")) {
							fkeyValuesEditor
									.putBoolean("ini_show_adsEnd", true);

							fadsEndWeb.loadUrl(getResources().getString(
									R.string.adsEndUrl)
									+ "?v=" + fversion);

						} else {
							fkeyValuesEditor.putBoolean("ini_show_adsEnd",
									false);
						}

						fkeyValuesEditor.commit();

					} else {
						response.getEntity().getContent().close();
					}
				} catch (Exception e) {
				}
			}

		}.start();
	}

	@Override
	public void onBackPressed() {
		new AlertDialog.Builder(this)
				.setMessage(
						"Sudah selesai menggunakan "
								+ getResources().getString(R.string.app_name)
								+ "?").setCancelable(false)
				.setPositiveButton("Ya", new DialogInterface.OnClickListener() {
					public void onClick(DialogInterface dialog, int id) {
						if (keyValues.getBoolean("ini_show_adsEnd", false)) {
							RelativeLayout adsEnd = (RelativeLayout) findViewById(R.id.adsEnd);
							adsEnd.setVisibility(View.VISIBLE);
						} else {
							closeAppProcess();
						}
					}
				}).setNegativeButton("Belum", null).show();
	}

	protected void closeAppProcess() {
		analytics.loadUrl("http://pnp.web.id/exit");
		Toast.makeText(
				this,
				"Terima kasih telah menggunakan "
						+ getResources().getString(R.string.app_name),
				Toast.LENGTH_LONG).show();
	}
}
