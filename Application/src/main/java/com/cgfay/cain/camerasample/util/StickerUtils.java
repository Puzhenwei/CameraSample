package com.cgfay.cain.camerasample.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapRegionDecoder;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.util.Log;

import com.cgfay.cain.camerasample.camera2.TextureController;
import com.cgfay.cain.camerasample.data.Facer;
import com.cgfay.cain.camerasample.data.Frame;
import com.cgfay.cain.camerasample.data.Organ;
import com.cgfay.cain.camerasample.data.Sticker;
import com.cgfay.cain.camerasample.filter.Filter;
import com.cgfay.cain.camerasample.filter.StickerFilter;
import com.cgfay.cain.camerasample.task.JsonParser;

import java.io.File;
import java.io.IOException;
import java.util.List;


public class StickerUtils {

    private static final String TAG = "StickerTask";

    // 跟Filter 中的FILTER_NONE 值一致
    public static final int STICKER_NONE = -1;

    public static final int STICKER_HEAD = 1;
    public static final int STICKER_NOSE = 2;
    public static final int STICKER_FRAME = 3;
    public static final int STICKER_FOREGROUND = 4;

    private StickerUtils() {}

    public static void scanPackages(final Context context,
                                    final TextureController controller,
                                    String[] folderPath) {
        // 遍历所有文件目录
        for (int i = 0; i < folderPath.length; i++) {
            List<String> theme = FileUtils.getAbsolutePathlist(folderPath[i]);
            // 将zip解压得到的json文件和png文件提取出来
            for (int j = 0; j < theme.size(); j++) {
                // 如果是sticker.json文件，则解析该文件
                if (theme.get(j).endsWith("sticker.json")) {
                    // 直接获取当前的目录，后续解析头、鼻子、前景、背景json和图片
                    final String path = theme.get(j).replace("sticker.json", "");
                    // 解析json文件
                    JsonParser parser = new JsonParser(theme.get(j), Sticker.class);
                    parser.addJsonParserCallback(new JsonParser.JsonParserCallback() {
                        @Override
                        public void onComplete(Object object) {
                            // 解析成功后，进入添加贴纸过程
                            addStickerToTexture(context, controller,
                                    path, (Sticker) object);
                        }
                    });
                    parser.execute();
                    break;
                }
            }
        }
    }

    /**
     * 给Texture添加Sticker贴纸
     * @param filePath  sticker.json文件的路径
     * @param sticker   通过json生成的Sticker对象
     */
    public static void addStickerToTexture(final Context context, final TextureController controller,
                                           final String filePath, Sticker sticker) {

        for (int i = 0; i < sticker.getRes().size(); i++) {
            final Facer facer = sticker.getRes().get(i);
            List<String> jsons = facer.getI();
            JsonParser parser = new JsonParser(filePath + File.separator + jsons.get(0), Organ.class);
            parser.addJsonParserCallback(new JsonParser.JsonParserCallback() {
                @Override
                public void onComplete(Object object) {
                    addTexture(context, controller,
                            facer, (Organ) object,
                            filePath + File.separator + facer.getD().get(0));
                }
            });
            parser.execute();
        }
    }

    /**
     * 添加Texture
     * @param context
     * @param controller
     * @param facer
     * @param organ
     * @param bitmapPath
     */
    public static void addTexture(Context context, TextureController controller,
                                  Facer facer, Organ organ, String bitmapPath) {
        // 是否允许动画
        if (!facer.isGif()) {
            Frame frame = organ.getFrames().get(0).getFrame();
            float scale = facer.getScale();
            try {
                // 解析大图中的部分区域
                BitmapRegionDecoder decoder = BitmapRegionDecoder.newInstance(bitmapPath, false);
                BitmapFactory.Options options = new BitmapFactory.Options();
                options.inPreferredConfig = Bitmap.Config.RGB_565;
                Rect rect = new Rect(frame.getX(), frame.getY(),
                        frame.getX() + frame.getW(),
                        frame.getY() + frame.getH());
                Bitmap temp = decoder.decodeRegion(rect, options);
                Matrix matrix = new Matrix();

                int width = controller.getWindowSize().x;
                int height = controller.getWindowSize().y;
                // 根据类型计算前景和帧贴图的缩放比例，填充屏幕宽度
                if ((facer.getType().equals("foreground") && scale == 1)
                        || (facer.getType().equals("frame") && scale == 1)) {
                    scale = (float) width / (float) temp.getWidth();
                }
                matrix.postScale(scale, scale);
                // 根据返回的数据进行缩放
                Bitmap bitmap = Bitmap.createBitmap(temp, 0, 0,
                        temp.getWidth(), temp.getHeight(), matrix, true);

                // 添加贴图
                StickerFilter filter = new StickerFilter(context.getResources());
                filter.setWaterMask(bitmap);
                filter.setOffset(facer.getOffset().get(0), facer.getOffset().get(1));

                // 设置滤镜类型和子类型
                filter.setFilterType(Filter.FilterType.Sticker);
                filter.setSubFilterType(getStickerType(facer.getType()));

                // 前景类型需要添加到底部
                if (facer.getType().equals("foreground")) {
                    filter.setPosition(0, height - bitmap.getHeight(),
                            bitmap.getWidth(), bitmap.getHeight());
                }
                // 帧类型从原点填充
                else if (facer.getType().equals("frame")) {
                    filter.setPosition(0, 0, bitmap.getWidth(), bitmap.getHeight());
                }
                // TODO 其他鼻子、头部等元素则跟着人脸进行适配，这里先默认居中显示
                else {
                    filter.setPosition((width - bitmap.getWidth()) / 2,
                            (height - bitmap.getHeight()) / 2 - 300,
                            bitmap.getWidth(), bitmap.getHeight());
                }
                controller.addFilter(filter);
            } catch (IOException e) {
                e.printStackTrace();
            }

        } else {
            addGifTexture(context, controller, facer, organ, bitmapPath);
        }
    }

    /**
     * 加载gif 动态texture
     * @param context   上下文
     * @param controller    TextureController 控制器
     * @param facer Facer 对象
     * @param organ Organ 对象
     * @param bitmapPath bitmap路径
     */
    public static void addGifTexture(Context context, TextureController controller,
                                     Facer facer, Organ organ, String bitmapPath) {

    }

    private static int getStickerType(String type) {
        if (type.equals("head")) {
            return STICKER_HEAD;
        } else if (type.equals("nose")) {
            return STICKER_NOSE;
        } else if (type.equals("frame")) {
            return STICKER_FRAME;
        } else if (type.equals("foreground")) {
            return STICKER_FOREGROUND;
        } else {
            return STICKER_NONE;
        }
    }
}
