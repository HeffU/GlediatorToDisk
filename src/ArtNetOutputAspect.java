import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import java.awt.Color;
import java.io.IOException;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Created by Heff on 2015-01-28.
 */
@Aspect
public class ArtNetOutputAspect {

    private boolean socketOpen = false;
    private Path path;
    private int size_x;
    private int size_y;
    private int num_unis;
    private int[] data_length;
    private int[][] patch_map;
    private byte[] buffer;
    private byte[] output;

    @Around("execution(public void *.ArtnetOutput.set_parameters(..))")
    public Object interceptSetParameters(ProceedingJoinPoint invocationPoint) throws Throwable {

        int[][] _unis = (int[][])invocationPoint.getArgs()[0];
        int[][][] _map = (int[][][])invocationPoint.getArgs()[1];

        num_unis = _unis.length;
        size_x = _map.length;
        size_y = _map[0].length;

        patch_map = new int[num_unis][1];
        data_length = new int[num_unis];

        for (int i = 0; i < this.num_unis; i++)
        {
            data_length[i] = _unis[i][7];
            patch_map[i] = new int[data_length[i]];
            for (int j = 0; j < data_length[i]; j++) {
                patch_map[i][j] = -1;
            }
        }
        for (int x = 0; x < this.size_x; x++) {
            for (int y = 0; y < this.size_y; y++)
            {
                int uni_index = _map[x][y][0];
                int ch_r = _map[x][y][1];
                int ch_g = _map[x][y][2];
                int ch_b = _map[x][y][3];

                patch_map[uni_index][ch_r] = ((y * this.size_x + x) * 3 + 0);
                patch_map[uni_index][ch_g] = ((y * this.size_x + x) * 3 + 1);
                patch_map[uni_index][ch_b] = ((y * this.size_x + x) * 3 + 2);
            }
        }

        buffer = new byte[size_x * size_y * 3];
        output = new byte[size_x * size_y * 3];

        return invocationPoint;
    }

    @Around("execution(public String *.ArtnetOutput.startArtnet())")
    public Object interceptOpenSocket(ProceedingJoinPoint invocationPoint) throws Throwable {

        Calendar cal = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd__hh-mm-ss");
        String dateStr = dateFormat.format(cal.getTime());
        path = Paths.get("PiLED_tools\\output", dateStr + ".led");
        Files.createFile(path);

        byte[] size = new byte[2];
        size[0] = (byte)size_x;
        size[1] = (byte)size_y;
        Files.write(path, size, StandardOpenOption.APPEND);

        socketOpen = true;
        return "Recording to: " + path.getFileName().toString();
    }

    @Around("execution(public String *.ArtnetOutput.stopArtnet())")
    public Object interceptCloseSocket(ProceedingJoinPoint invocationPoint) throws Throwable {

        socketOpen = false;
        return "Recorded to: " + path.getFileName().toString();
    }

    @Around("execution(public boolean *.ArtnetOutput.get_artnet_status())")
    public Object interceptStatus(ProceedingJoinPoint invocationPoint) throws Throwable {

        return socketOpen;
    }

    @Around("execution(public void *.ArtnetOutput.send_out_one_frame(..))")
    public Object interceptAndSaveFrame(ProceedingJoinPoint invocationPoint) throws Throwable {

        if(!socketOpen)
            return invocationPoint;

        Color[] frame = (Color[])invocationPoint.getArgs()[0];
        appendFrameToFile(frame);

        return invocationPoint;
    }

    public void appendFrameToFile(Color[] frame) throws IOException {

        // Sanity Check to avoid Glediator's threading issues
        if (size_x*size_y <=0 || size_x*size_y > frame.length*3)
            return;

        for (int x = 0; x < size_x; x++) {
            for (int y = 0; y < size_y; y++)
            {
                int index = y * size_x + x;

                buffer[(3 * index + 0)] = ((byte)frame[index].getRed());
                buffer[(3 * index + 1)] = ((byte)frame[index].getGreen());
                buffer[(3 * index + 2)] = ((byte)frame[index].getBlue());
            }
        }
        int offset = 0;
        for (int uni = 0; uni < this.num_unis; uni++)
        {
            int count = 0;
            for (int channel = 0; channel < data_length[uni]; channel++)
            {
                int position = patch_map[uni][channel];
                if (position != -1) {
                    output[offset+count] = buffer[position];
                    count++;
                }
            }
            offset += count;
        }
        Files.write(path, buffer, StandardOpenOption.APPEND);
    }
}