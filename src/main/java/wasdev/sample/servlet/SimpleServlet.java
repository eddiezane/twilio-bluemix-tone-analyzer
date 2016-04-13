package wasdev.sample.servlet;

import com.ibm.watson.developer_cloud.speech_to_text.v1.RecognizeOptions;
import com.ibm.watson.developer_cloud.speech_to_text.v1.SpeechToText;
import com.ibm.watson.developer_cloud.speech_to_text.v1.model.SpeechResults;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.ToneAnalyzer;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneAnalysis;
import com.ibm.watson.developer_cloud.tone_analyzer.v3.model.ToneScore;
import com.twilio.sdk.verbs.*;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Servlet implementation class SimpleServlet
 */
@WebServlet("/SimpleServlet")
public class SimpleServlet extends HttpServlet {
    private static final long serialVersionUID = 1L;

    /**
     * @see HttpServlet#doGet(HttpServletRequest request, HttpServletResponse response)
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        TwiMLResponse twiMLResponse = new TwiMLResponse();
        Say say;
        Record record;

        try {
            say = new Say("Please tell me what you are calling about then press pound.");

            record = new Record();
            record.setMethod("POST");
            record.setFinishOnKey("#");

            twiMLResponse.append(say);
            twiMLResponse.append(record);

            response.setContentType("text/xml");
            response.getWriter().print(twiMLResponse.toXML());
        } catch (TwiMLException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        String recordingUrl = request.getParameter("RecordingUrl");

        if (recordingUrl == null) {
            return;
        }

        URL url = new URL(recordingUrl);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        InputStream inputStream = connection.getInputStream();

        Path tempFile = Files.createTempFile("recording", ".wav");
        Files.copy(inputStream, tempFile, StandardCopyOption.REPLACE_EXISTING);

        SpeechToText speechToText = new SpeechToText();

        RecognizeOptions recognizeOptions = new RecognizeOptions();
        recognizeOptions.model("en-US_NarrowbandModel");

        SpeechResults speechResults = speechToText.recognize(tempFile.toFile(), recognizeOptions);
        String transcription = speechResults.getResults().get(0).getAlternatives().get(0).getTranscript();

        ToneAnalyzer toneAnalyzer = new ToneAnalyzer(ToneAnalyzer.VERSION_DATE_2016_02_11);

        ToneAnalysis toneAnalysis = toneAnalyzer.getTone(transcription);

        StringBuilder toneString = new StringBuilder();

        for (ToneScore toneScore : toneAnalysis.getDocumentTone().getTones().get(0).getTones()) {
            String t = toneScore.getName() + ": " + toneScore.getScore() + "\n";
            toneString.append(t);
        }

        TwiMLResponse twiMLResponse = new TwiMLResponse();
        Sms sms = new Sms(toneString.toString());

        try {
            twiMLResponse.append(sms);
            response.setContentType("text/xml");
            response.getWriter().print(twiMLResponse.toXML());
        } catch (TwiMLException e) {
            e.printStackTrace();
        }

        Files.delete(tempFile);
    }
}
