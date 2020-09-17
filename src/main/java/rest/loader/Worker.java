package rest.loader;

import java.util.concurrent.Callable;

import javax.net.ssl.SSLContext;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.AuthCache;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.protocol.HttpClientContext;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.auth.BasicScheme;
import org.apache.http.impl.client.BasicAuthCache;
import org.apache.http.impl.client.BasicCredentialsProvider;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContexts;
import org.apache.http.util.EntityUtils;

public class Worker implements Callable<Long> {

	public static CredentialsProvider provider = new BasicCredentialsProvider();

	public static AuthCache authCache = new BasicAuthCache();

	public static HttpClientContext localContext = HttpClientContext.create();

	public static String operationType = "POST";
	
	public static boolean exceptionHappened = false;

	@Override
	public Long call() {
		
		Long result = null;
		
		try {

			SSLContext sslContext = SSLContexts.custom().loadTrustMaterial(new TrustSelfSignedStrategy()).build();

			SSLConnectionSocketFactory sslSF = new SSLConnectionSocketFactory(sslContext,
					SSLConnectionSocketFactory.getDefaultHostnameVerifier());

			HttpHost target = new HttpHost(App.getProperties("qm.host"), Integer.valueOf(App.getProperties("qm.port")),
					"https");

			provider.setCredentials(new AuthScope(target.getHostName(), target.getPort()),
					new UsernamePasswordCredentials(App.getProperties("qm.user"), App.getProperties("qm.password")));

			authCache.put(target, new BasicScheme());

			localContext.setAuthCache(authCache);

			CloseableHttpClient httpclient = HttpClients.custom().setSSLSocketFactory(sslSF)
					.setDefaultCredentialsProvider(provider).build();
			
			String url = "https://" + App.getProperties("qm.host") + ":" + Integer.valueOf(App.getProperties("qm.port")) + "/ibmmq/rest/v2/messaging/qmgr/"
						 + App.getProperties("qm.name") + "/queue/" + App.getProperties("qm.queue.name") + "/message";
			
			//LOGIN
			// HttpGet httpLoginRequest = new HttpGet("https://localhost:9443/ibmmq/rest/v2/login");
			//GET
			// HttpGet httpGetRequest = new HttpGet(url);
			HttpDelete httpDeleteRequest = new HttpDelete(url);
			HttpPost httpPostRequest = new HttpPost(url);
			CloseableHttpResponse response;
			
			if (App.getProperties("operation.type").toLowerCase().equals("delete")) {
				operationType = "DELETE";
				httpDeleteRequest.setHeader("ibm-mq-rest-csrf-token", "blank");
			} else {
				operationType = "POST";
				httpPostRequest.setHeader("ibm-mq-rest-csrf-token", "blank");
				httpPostRequest.setHeader("Content-Type", "text/plain;charset=utf-8");
				httpPostRequest.setEntity(
						new StringEntity(App.generateString(Integer.valueOf(App.getProperties("loader.payload.length")))));
			}

			try {

				// System.out.println("Executing request " + httpRequest.getRequestLine());

				int loadRequestsCount = Integer.valueOf(App.getProperties("loader.cnt"));
				
				//System.out.println("Start sending");
				
				Long startTime = System.currentTimeMillis();

				for (int i = 0; i < loadRequestsCount; i++) {

					switch (operationType) {
					case "DELETE":
						response = httpclient.execute(target, httpDeleteRequest, localContext);
						break;
					default:
						response = httpclient.execute(target, httpPostRequest, localContext);
					}
					
					try {
						
						HttpEntity entity = response.getEntity();
						//printResponseStatus(response);
						EntityUtils.consume(entity);
						
					} catch (Exception e) {
						
						System.out.println("Exception during handling a response " + e.getMessage());
						exceptionHappened = true;
						
					} finally {
						response.close();
					}
				}
				
				Long endTime = System.currentTimeMillis();
				
				result = endTime - startTime;
				
			} catch (Exception e) {
				
				System.out.println("Exception during execution of request " + e.getMessage());
				exceptionHappened = true;
				
			} finally {
				
				httpclient.close();
			}

		} catch (Exception e) {

			System.out.println("Exception during Worker execution " + e.getMessage());
			exceptionHappened = true;
					
		}
		
		return result;

	}

}
