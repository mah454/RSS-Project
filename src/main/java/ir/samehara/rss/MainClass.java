package ir.samehara.rss;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.FeedException;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import jcifs.smb.NtlmPasswordAuthentication;
import jcifs.smb.SmbFile;
import jcifs.smb.SmbFileOutputStream;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Stream;

/*
 * smbPath=smb://w.x.y.z/hr/FILE.CSV
 * username=[USERNAME]
 * password=[PASSWORD]
 *
 * */


public class MainClass {
    public static void main(String[] args) throws Exception {
        Properties properties = ConfigurationReader.getConfig.getConfig();
        String smbPath = properties.getProperty("smbPath");
        String username = properties.getProperty("username");
        String password = properties.getProperty("password");

        NtlmPasswordAuthentication authentication = new NtlmPasswordAuthentication(username + ":" + password);
        SmbFile smbFile = new SmbFile(smbPath, authentication);

        if (smbFile.exists()) {
            smbFile.delete();
        }

        Path path = Paths.get("etc/links.txt");
        Stream<String> links = getLinks(path);
        String csv = csvGenerator(links);

        SmbFileOutputStream smbFileOutputStream = new SmbFileOutputStream(smbFile, true);
        smbFileOutputStream.write(csv.getBytes());
        smbFileOutputStream.flush();
        smbFileOutputStream.close();
    }

    private static String csvGenerator(Stream<String> links) {
        StringBuilder line = new StringBuilder();
        links.forEach(l -> {
            try (CloseableHttpClient client = HttpClients.createMinimal()) {
                HttpUriRequest request = new HttpGet(l);
                System.out.println("URL :> " + request.getURI());
                try (CloseableHttpResponse response = client.execute(request);
                     InputStream inputStream = response.getEntity().getContent()) {
                    SyndFeedInput input = new SyndFeedInput();
                    SyndFeed feed = input.build(new XmlReader(inputStream));
                    Optional<SyndEntry> syndEntry = feed.getEntries().stream().findFirst();
                    if (syndEntry.isPresent()) {
                        SyndEntry s = syndEntry.get();

                        DateFormat dateFormat = new SimpleDateFormat("yyyyMMdd");
                        String date = dateFormat.format(s.getPublishedDate());
                        String currency = s.getCategories().get(0).getName();
                        String value = s.getDescription().getValue();
                        line.append("M" + ",")
                                .append(currency)
                                .append(",")
                                .append("IRR")
                                .append(",")
                                .append(date)
                                .append(",")
                                .append(value).append("\n");
                    }
                } catch (FeedException e) {
                    e.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
        return line.toString();
    }

    private static Stream<String> getLinks(Path path) throws IOException {
        return Files.lines(path);
    }
}
