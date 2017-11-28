package org.openbravo.retail.posterminal.utility;

import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import javax.imageio.ImageIO;

import org.apache.log4j.Logger;
import org.hibernate.ScrollableResults;
import org.hibernate.criterion.Restrictions;
import org.openbravo.base.ConfigParameters;
import org.openbravo.client.kernel.RequestContext;
import org.openbravo.dal.service.OBCriteria;
import org.openbravo.dal.service.OBDal;
import org.openbravo.erpCommon.utility.MimeTypeUtil;
import org.openbravo.erpCommon.utility.OBError;
import org.openbravo.erpCommon.utility.OBMessageUtils;
import org.openbravo.erpCommon.utility.Utility;
import org.openbravo.model.pricing.priceadjustment.PriceAdjustment;
import org.openbravo.model.pricing.priceadjustment.PromotionType;
import org.openbravo.retail.config.OBRETCOProductList;
import org.openbravo.retail.config.OBRETCOProlProduct;
import org.openbravo.scheduling.ProcessBundle;
import org.openbravo.service.db.DalBaseProcess;

public class GenerateProductImages extends DalBaseProcess {

  private static final Logger log4j = Logger.getLogger(GenerateProductImages.class);

  public void doExecute(ProcessBundle bundle) throws Exception {
    try {

      // retrieve the parameters from the bundle
      final String assortmentId = (String) bundle.getParams().get("obretcoProductlistId");

      // implement your process here

      final OBRETCOProductList assortment = OBDal.getInstance().get(OBRETCOProductList.class,
          assortmentId);

      ConfigParameters confParam = ConfigParameters
          .retrieveFrom(RequestContext.getServletContext());

      File imagesDir = new File(confParam.prefix
          + "web/org.openbravo.retail.posterminal/productImages/");
      if (!imagesDir.exists()) {
        imagesDir.mkdirs();
      }

      final StringBuilder errors = new StringBuilder();
      int errorCounter = 0;

      OBCriteria<OBRETCOProlProduct> prolProductCrit = OBDal.getInstance().createCriteria(
          OBRETCOProlProduct.class);
      prolProductCrit.add(Restrictions.eq(OBRETCOProlProduct.PROPERTY_OBRETCOPRODUCTLIST,
          assortment));
      ScrollableResults prolProductScroll = prolProductCrit.scroll();
      while (prolProductScroll.next()) {
        OBRETCOProlProduct prolProduct = (OBRETCOProlProduct) prolProductScroll.get(0);
        if (prolProduct.getProduct().getImage() != null) {
          try {
            generateImageFile(prolProduct.getProduct().getId(), prolProduct.getProduct().getImage()
                .getId(), imagesDir);
          } catch (Exception ex) {
            if (errorCounter < 30) {
              String error = OBMessageUtils.getI18NMessage("OBPOS_ProductCanNotBeResized",
                  new String[] { prolProduct.getProduct().getIdentifier() })
                  + " - "
                  + ex.getMessage();
              errors.append(error + "<br/>");
              log4j.error(error, ex);
            } else if (errorCounter == 30) {
              errors.append(OBMessageUtils.getI18NMessage("OBPOS_AndMore", null));
            }
            errorCounter++;
          }
        }
        OBDal.getInstance().getSession().clear();
      }

      OBCriteria<PriceAdjustment> packs = OBDal.getInstance().createCriteria(PriceAdjustment.class);
      packs.add(Restrictions.eq(PriceAdjustment.PROPERTY_DISCOUNTTYPE,
          OBDal.getInstance().get(PromotionType.class, "BE5D42E554644B6AA262CCB097753951")));
      packs.add(Restrictions.isNotNull(PriceAdjustment.PROPERTY_OBDISCIMAGE));
      for (PriceAdjustment pack : packs.list()) {
        try {
          generateImageFile(pack.getId(), pack.getObdiscImage().getId(), imagesDir);
        } catch (Exception ex) {
          if (errorCounter < 30) {
            String error = OBMessageUtils.getI18NMessage("OBPOS_ProductCanNotBeResized",
                new String[] { pack.getIdentifier() }) + " - " + ex.getMessage();
            errors.append(error + "<br/>");
            log4j.error(error, ex);
          } else if (errorCounter == 30) {
            errors.append(OBMessageUtils.getI18NMessage("OBPOS_AndMore", null));
          }
          errorCounter++;
        }
      }

      // Show a result
      final StringBuilder sb = new StringBuilder();
      sb.append(OBMessageUtils.getI18NMessage("OBPOS_ProductImageCreated", null) + "<br/>");
      if (errors != null && !errors.equals("")) {
        sb.append(errors);
      }

      // OBError is also used for successful results
      final OBError msg = new OBError();
      msg.setType("Success");
      msg.setTitle("Read parameters!");
      msg.setMessage(sb.toString());

      bundle.setResult(msg);

    } catch (final Exception e) {
      e.printStackTrace(System.err);
      final OBError msg = new OBError();
      msg.setType("Error");
      msg.setMessage(e.getMessage());
      msg.setTitle("Error occurred");
      bundle.setResult(msg);
    }
  }

  private void generateImageFile(String id, String imageId, File imagesDir) throws Exception {

    byte[] img = Utility.getImage(imageId);

    img = Utility.resizeImageByte(img, 160, 0, true, true);
    img = resizeImageByteToSquare(img);

    if (img != null) {
      String imageDir = getProductImage(imagesDir, id);
      File f = new File(imageDir, id);
      f.createNewFile();
      FileOutputStream is = new FileOutputStream(f);
      is.write(img);
      is.close();
    }
  }

  private String getProductImage(File imagesDir, String origname) {
    String newname = imagesDir + "/";
    for (int i = 0; i < origname.length(); i += 3) {
      if (i != 0) {
        newname += "/";
      }
      newname += origname.substring(i, Math.min(i + 3, origname.length()));
    }
    File dir = new File(newname);
    if (!dir.exists()) {
      dir.mkdirs();
    }
    return newname;
  }

  public static byte[] resizeImageByteToSquare(byte[] bytea) throws IOException {
    ByteArrayInputStream bis = new ByteArrayInputStream(bytea);
    BufferedImage rImage = ImageIO.read(bis);
    int oldW = rImage.getWidth();
    int oldH = rImage.getHeight();

    int scaledWidth;
    int scaledHeight;

    int newW;
    int newH;
    if (oldW >= oldH) {
      newW = oldW;
      newH = oldW;
    } else {
      newW = oldH;
      newH = oldH;
    }

    if (oldW == 0 || oldH == 0 || (oldW == newW && oldH == newH)) {
      return bytea;
    } else if (oldW == oldH) {
      scaledWidth = newW;
      scaledHeight = newH;
    } else if (oldW >= oldH) {
      scaledWidth = newW;
      double scale = (double) newW / (double) oldW;
      scaledHeight = (int) Math.round(oldH * scale);
    } else {
      scaledHeight = newH;
      double scale = (double) newH / (double) oldH;
      scaledWidth = (int) Math.round(oldW * scale);
    }

    int x = (newW - scaledWidth) / 2;
    int y = (newH - scaledHeight) / 2;

    BufferedImage dimg = new BufferedImage(newW, newH, rImage.getType());
    Graphics2D g = dimg.createGraphics();
    g.setBackground(Color.WHITE);
    g.clearRect(0, 0, newW, newH);
    g.setRenderingHint(RenderingHints.KEY_INTERPOLATION,
        RenderingHints.VALUE_INTERPOLATION_BILINEAR);
    g.drawImage(rImage, x, y, x + scaledWidth, y + scaledHeight, 0, 0, oldW, oldH, Color.WHITE,
        null);
    g.dispose();
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    String mimeType = MimeTypeUtil.getInstance().getMimeTypeName(bytea);
    if (mimeType.contains("jpeg")) {
      mimeType = "jpeg";
    } else if (mimeType.contains("png")) {
      mimeType = "png";
    } else if (mimeType.contains("gif")) {
      mimeType = "gif";
    } else if (mimeType.contains("bmp")) {
      mimeType = "bmp";
    } else {
      return bytea;
    }
    ImageIO.write(dimg, mimeType, baos);
    byte[] bytesOut = baos.toByteArray();
    return bytesOut;
  }
}