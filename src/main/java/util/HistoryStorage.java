package util;

import entities.AppSettings;
import services.AppSettingsService;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.TimeZone;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * Abstraction around the storage backend that holds historical PDF / DOCX /
 * TXT files (planning + affectation snapshots).
 *
 * <p>Two backends are supported:</p>
 * <ul>
 *   <li><b>LOCAL</b> – plain folder on the filesystem (configured by the user).</li>
 *   <li><b>S3</b> – any S3-compatible object storage (AWS S3, MinIO, Wasabi,
 *       Backblaze B2, Cloudflare R2, ...). Implemented using AWS SigV4 over
 *       plain {@link java.net.HttpURLConnection} so we don't need any extra
 *       dependencies.</li>
 * </ul>
 *
 * <p>The active backend is resolved from {@link AppSettings} on every call so
 * the user can switch backends at runtime via the settings page.</p>
 */
public final class HistoryStorage {

    private static final Logger LOG = Logger.getLogger(HistoryStorage.class.getName());
    private static final HistoryStorage INSTANCE = new HistoryStorage();

    private HistoryStorage() {}

    public static HistoryStorage getInstance() {
        return INSTANCE;
    }

    // ─── Public API ─────────────────────────────────────────────────────────

    /**
     * Lists every file currently stored, sorted by name in reverse order
     * (matches the existing UI which expects most recent first thanks to the
     * timestamp in the file name).
     */
    public List<String> listFiles() {
        AppSettings s = AppSettingsService.getInstance().get();
        try {
            if (s.getStorageMode() == AppSettings.StorageMode.S3) {
                return listS3(s);
            }
            return listLocal(s);
        } catch (Exception e) {
            LOG.log(Level.WARNING, "HistoryStorage list failed", e);
            return new ArrayList<>();
        }
    }

    public List<String> listFilesWithPrefix(String prefix) {
        if (prefix == null || prefix.isEmpty()) return listFiles();
        List<String> out = new ArrayList<>();
        for (String name : listFiles()) {
            if (name.startsWith(prefix)) out.add(name);
        }
        return out;
    }

    public boolean exists(String fileName) {
        if (fileName == null || fileName.isEmpty()) return false;
        AppSettings s = AppSettingsService.getInstance().get();
        try {
            if (s.getStorageMode() == AppSettings.StorageMode.S3) {
                return existsS3(s, fileName);
            }
            return new File(localFolder(s), fileName).isFile();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "HistoryStorage exists failed for " + fileName, e);
            return false;
        }
    }

    public byte[] read(String fileName) throws IOException {
        Objects.requireNonNull(fileName, "fileName");
        AppSettings s = AppSettingsService.getInstance().get();
        if (s.getStorageMode() == AppSettings.StorageMode.S3) {
            return readS3(s, fileName);
        }
        return Files.readAllBytes(new File(localFolder(s), fileName).toPath());
    }

    public InputStream openInputStream(String fileName) throws IOException {
        AppSettings s = AppSettingsService.getInstance().get();
        if (s.getStorageMode() == AppSettings.StorageMode.S3) {
            return new java.io.ByteArrayInputStream(readS3(s, fileName));
        }
        return Files.newInputStream(new File(localFolder(s), fileName).toPath());
    }

    public void write(String fileName, byte[] data, String contentType) throws IOException {
        Objects.requireNonNull(fileName, "fileName");
        Objects.requireNonNull(data, "data");
        AppSettings s = AppSettingsService.getInstance().get();
        if (s.getStorageMode() == AppSettings.StorageMode.S3) {
            putS3(s, fileName, data, contentType == null ? "application/octet-stream" : contentType);
            return;
        }
        File dir = localFolder(s);
        if (!dir.exists()) dir.mkdirs();
        Files.write(new File(dir, fileName).toPath(), data);
    }

    public void delete(String fileName) {
        if (fileName == null || fileName.isEmpty()) return;
        AppSettings s = AppSettingsService.getInstance().get();
        try {
            if (s.getStorageMode() == AppSettings.StorageMode.S3) {
                deleteS3(s, fileName);
                return;
            }
            File f = new File(localFolder(s), fileName);
            if (f.isFile()) f.delete();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "HistoryStorage delete failed for " + fileName, e);
        }
    }

    /**
     * Returns a quick health-check string describing the current backend.
     * Used by the settings page to give the user fast feedback.
     */
    public String describeStatus() {
        AppSettings s = AppSettingsService.getInstance().get();
        if (s.getStorageMode() == AppSettings.StorageMode.S3) {
            return "Cloud (S3): " + safe(s.getS3Bucket()) + "@" + safe(s.getS3Endpoint());
        }
        return "Local: " + localFolder(s).getAbsolutePath();
    }

    public boolean testConnection() {
        AppSettings s = AppSettingsService.getInstance().get();
        try {
            if (s.getStorageMode() == AppSettings.StorageMode.S3) {
                listS3(s); // throws on auth/network failure
                return true;
            }
            File dir = localFolder(s);
            if (!dir.exists() && !dir.mkdirs()) return false;
            return dir.canRead() && dir.canWrite();
        } catch (Exception e) {
            LOG.log(Level.WARNING, "HistoryStorage testConnection failed", e);
            return false;
        }
    }

    // ─── LOCAL implementation ──────────────────────────────────────────────

    private File localFolder(AppSettings s) {
        String configured = s == null ? null : s.getLocalStoragePath();
        if (configured == null || configured.trim().isEmpty()) {
            configured = System.getProperty("user.home") + File.separator + "plannings_history";
        }
        return new File(configured);
    }

    private List<String> listLocal(AppSettings s) {
        File dir = localFolder(s);
        if (!dir.isDirectory()) return new ArrayList<>();
        File[] files = dir.listFiles();
        if (files == null) return new ArrayList<>();
        List<String> out = new ArrayList<>();
        for (File f : files) {
            if (f.isFile()) out.add(f.getName());
        }
        Collections.sort(out, Collections.reverseOrder());
        return out;
    }

    // ─── S3 implementation (SigV4, no SDK needed) ──────────────────────────

    private static String safe(String v) { return v == null ? "" : v; }

    private static String stripSlashes(String v) {
        if (v == null) return "";
        String x = v.trim();
        while (x.startsWith("/")) x = x.substring(1);
        while (x.endsWith("/")) x = x.substring(0, x.length() - 1);
        return x;
    }

    private String objectKey(AppSettings s, String fileName) {
        String prefix = stripSlashes(s.getS3Prefix());
        return prefix.isEmpty() ? fileName : prefix + "/" + fileName;
    }

    private List<String> listS3(AppSettings s) throws IOException {
        String prefix = stripSlashes(s.getS3Prefix());
        String query = "list-type=2";
        if (!prefix.isEmpty()) {
            query += "&prefix=" + URLEncoder.encode(prefix + "/", StandardCharsets.UTF_8)
                    .replace("+", "%20");
        }
        S3Response resp = signedRequest(s, "GET", "", query, null, null);
        if (resp.status / 100 != 2) {
            throw new IOException("S3 list failed: HTTP " + resp.status + " - " + new String(resp.body, StandardCharsets.UTF_8));
        }

        List<String> keys = new ArrayList<>();
        try {
            javax.xml.parsers.DocumentBuilder builder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
            org.w3c.dom.Document doc = builder.parse(new InputSource(new java.io.ByteArrayInputStream(resp.body)));
            XPath xpath = XPathFactory.newInstance().newXPath();
            NodeList nodes = (NodeList) xpath.evaluate("//*[local-name()='Contents']/*[local-name()='Key']/text()",
                    doc, XPathConstants.NODESET);
            for (int i = 0; i < nodes.getLength(); i++) {
                String key = nodes.item(i).getNodeValue();
                if (key == null) continue;
                String name = stripPrefixFromKey(prefix, key);
                if (name != null && !name.isEmpty() && !name.endsWith("/")) {
                    keys.add(name);
                }
            }
        } catch (Exception e) {
            throw new IOException("Could not parse S3 list response", e);
        }
        Collections.sort(keys, Collections.reverseOrder());
        return keys;
    }

    private static String stripPrefixFromKey(String prefix, String key) {
        if (prefix == null || prefix.isEmpty()) return key;
        String pfx = prefix + "/";
        return key.startsWith(pfx) ? key.substring(pfx.length()) : key;
    }

    private boolean existsS3(AppSettings s, String fileName) throws IOException {
        S3Response resp = signedRequest(s, "HEAD", objectKey(s, fileName), null, null, null);
        return resp.status / 100 == 2;
    }

    private byte[] readS3(AppSettings s, String fileName) throws IOException {
        S3Response resp = signedRequest(s, "GET", objectKey(s, fileName), null, null, null);
        if (resp.status / 100 != 2) {
            throw new IOException("S3 GET failed: HTTP " + resp.status);
        }
        return resp.body;
    }

    private void putS3(AppSettings s, String fileName, byte[] data, String contentType) throws IOException {
        S3Response resp = signedRequest(s, "PUT", objectKey(s, fileName), null, data, contentType);
        if (resp.status / 100 != 2) {
            throw new IOException("S3 PUT failed: HTTP " + resp.status + " - " + new String(resp.body, StandardCharsets.UTF_8));
        }
    }

    private void deleteS3(AppSettings s, String fileName) throws IOException {
        S3Response resp = signedRequest(s, "DELETE", objectKey(s, fileName), null, null, null);
        if (resp.status != 204 && resp.status / 100 != 2) {
            throw new IOException("S3 DELETE failed: HTTP " + resp.status);
        }
    }

    private static class S3Response {
        final int status;
        final byte[] body;
        S3Response(int status, byte[] body) { this.status = status; this.body = body; }
    }

    private S3Response signedRequest(AppSettings s, String method, String key, String query,
                                     byte[] body, String contentType) throws IOException {
        String endpoint = s.getS3Endpoint();
        String bucket = s.getS3Bucket();
        if (endpoint == null || endpoint.isEmpty() || bucket == null || bucket.isEmpty()) {
            throw new IOException("S3 endpoint or bucket is not configured.");
        }
        String region = (s.getS3Region() == null || s.getS3Region().isEmpty()) ? "us-east-1" : s.getS3Region();
        String access = s.getS3AccessKey();
        String secret = s.getS3SecretKey();
        if (access == null || access.isEmpty() || secret == null || secret.isEmpty()) {
            throw new IOException("S3 credentials are not configured.");
        }

        URL endpointUrl = new URL(endpoint);
        String host = endpointUrl.getHost();
        if (endpointUrl.getPort() != -1) host = host + ":" + endpointUrl.getPort();

        boolean pathStyle = s.getS3PathStyleAccess();
        String path;
        String hostHeader;
        if (pathStyle) {
            path = "/" + bucket + (key.isEmpty() ? "" : "/" + encodeKey(key));
            hostHeader = host;
        } else {
            path = "/" + (key.isEmpty() ? "" : encodeKey(key));
            hostHeader = bucket + "." + host;
        }

        byte[] payload = body == null ? new byte[0] : body;
        String payloadHash = hex(sha256(payload));

        SimpleDateFormat amzFmt = new SimpleDateFormat("yyyyMMdd'T'HHmmss'Z'");
        amzFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        SimpleDateFormat dateFmt = new SimpleDateFormat("yyyyMMdd");
        dateFmt.setTimeZone(TimeZone.getTimeZone("UTC"));
        Date now = new Date();
        String amzDate = amzFmt.format(now);
        String dateStamp = dateFmt.format(now);

        // Canonical request
        String canonicalQuery = canonicalizeQuery(query);
        StringBuilder canonicalHeaders = new StringBuilder();
        canonicalHeaders.append("host:").append(hostHeader).append('\n');
        canonicalHeaders.append("x-amz-content-sha256:").append(payloadHash).append('\n');
        canonicalHeaders.append("x-amz-date:").append(amzDate).append('\n');
        String signedHeaders = "host;x-amz-content-sha256;x-amz-date";

        String canonicalRequest = method + "\n"
                + path + "\n"
                + (canonicalQuery == null ? "" : canonicalQuery) + "\n"
                + canonicalHeaders + "\n"
                + signedHeaders + "\n"
                + payloadHash;

        String credentialScope = dateStamp + "/" + region + "/s3/aws4_request";
        String stringToSign = "AWS4-HMAC-SHA256\n" + amzDate + "\n"
                + credentialScope + "\n"
                + hex(sha256(canonicalRequest.getBytes(StandardCharsets.UTF_8)));

        byte[] kSecret  = ("AWS4" + secret).getBytes(StandardCharsets.UTF_8);
        byte[] kDate    = hmacSha256(kSecret, dateStamp);
        byte[] kRegion  = hmacSha256(kDate, region);
        byte[] kService = hmacSha256(kRegion, "s3");
        byte[] kSigning = hmacSha256(kService, "aws4_request");
        String signature = hex(hmacSha256(kSigning, stringToSign));

        String authHeader = "AWS4-HMAC-SHA256 Credential=" + access + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders
                + ", Signature=" + signature;

        // Build URL & connection
        String urlStr = endpointUrl.getProtocol() + "://" + (pathStyle ? host : (bucket + "." + host)) + path
                + (canonicalQuery == null || canonicalQuery.isEmpty() ? "" : "?" + canonicalQuery);
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod(method);
        conn.setConnectTimeout(15000);
        conn.setReadTimeout(60000);
        conn.setRequestProperty("Host", hostHeader);
        conn.setRequestProperty("x-amz-content-sha256", payloadHash);
        conn.setRequestProperty("x-amz-date", amzDate);
        conn.setRequestProperty("Authorization", authHeader);
        if (contentType != null) conn.setRequestProperty("Content-Type", contentType);

        if ("PUT".equals(method) || "POST".equals(method)) {
            conn.setDoOutput(true);
            conn.setFixedLengthStreamingMode(payload.length);
            try (OutputStream os = conn.getOutputStream()) {
                os.write(payload);
            }
        }

        int status;
        byte[] respBody;
        try {
            status = conn.getResponseCode();
            try (InputStream is = (status >= 400) ? conn.getErrorStream() : conn.getInputStream()) {
                respBody = is == null ? new byte[0] : readAll(is);
            }
        } finally {
            conn.disconnect();
        }
        return new S3Response(status, respBody);
    }

    private static byte[] readAll(InputStream is) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        byte[] buf = new byte[8192];
        int n;
        while ((n = is.read(buf)) >= 0) out.write(buf, 0, n);
        return out.toByteArray();
    }

    private static String encodeKey(String key) {
        // RFC3986: keep / unencoded for object keys but encode every segment.
        StringBuilder sb = new StringBuilder();
        for (String segment : key.split("/", -1)) {
            if (sb.length() > 0) sb.append('/');
            sb.append(URLEncoder.encode(segment, StandardCharsets.UTF_8)
                    .replace("+", "%20")
                    .replace("*", "%2A")
                    .replace("%7E", "~"));
        }
        return sb.toString();
    }

    private static String canonicalizeQuery(String query) {
        if (query == null || query.isEmpty()) return "";
        String[] parts = query.split("&");
        java.util.Arrays.sort(parts);
        return String.join("&", parts);
    }

    private static byte[] sha256(byte[] data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(data);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static byte[] hmacSha256(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static String hex(byte[] data) {
        StringBuilder sb = new StringBuilder(data.length * 2);
        for (byte b : data) {
            sb.append(String.format(Locale.ROOT, "%02x", b & 0xFF));
        }
        return sb.toString();
    }

    /**
     * Tries to figure out a content type from an extension. Used when we
     * upload to S3 so the file is served with the right MIME type.
     */
    public static String guessContentType(String fileName) {
        if (fileName == null) return "application/octet-stream";
        String lower = fileName.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".pdf"))  return "application/pdf";
        if (lower.endsWith(".docx")) return "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
        if (lower.endsWith(".xlsx")) return "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
        if (lower.endsWith(".txt"))  return "text/plain; charset=UTF-8";
        if (lower.endsWith(".zip"))  return "application/zip";
        if (lower.endsWith(".png"))  return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        return "application/octet-stream";
    }

    /**
     * Convenience: walks a path and returns its bytes. Used by
     * {@code restoreAffectation} which still needs a {@link File}.
     */
    public static byte[] readPath(Path p) throws IOException {
        return Files.readAllBytes(p);
    }
}
