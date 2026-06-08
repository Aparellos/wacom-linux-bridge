import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import java.util.List;
import java.util.ArrayList;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.image.BufferedImage;
import com.WacomGSS.STU.*;
import com.WacomGSS.STU.Protocol.*;
import java.util.Arrays;
import java.math.BigInteger;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.io.File;
import javax.imageio.ImageIO;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class WacomCapture 
{
  private Future<KeyPair> keyPair;
  private String signerName = "";
  private String signReason = "";
  private String imagePath = "sig.png";

  public WacomCapture(String signerName, String signReason, String imagePath) {
      this.signerName = signerName;
      this.signReason = signReason;
      if (imagePath != null && !imagePath.isEmpty()) {
          this.imagePath = imagePath;
      }
  }

  static class SignatureDialog extends JDialog implements ITabletHandler
  {
    static class MyEncryptionHandler implements Tablet.IEncryptionHandler
    {
      private BigInteger p;
      private BigInteger g;
      private BigInteger privateKey;
      private Cipher     aesCipher;
       
      @Override
      public void reset()
      {
        clearKeys();
        this.p = null;
        this.g = null;
      }

      @Override
      public void clearKeys()
      {
        this.privateKey = null;
        this.aesCipher = null;
      }

      @Override
      public boolean requireDH()
      {
        return this.p == null || this.g == null;
      }

      @Override
      public void setDH(DHprime dhPrime, DHbase dhBase)
      {
        this.p = new BigInteger(1, dhPrime.getValue());
        this.g = new BigInteger(1, dhBase.getValue());
      }

      @Override
      public com.WacomGSS.STU.Protocol.PublicKey generateHostPublicKey()
      {
        this.privateKey = new BigInteger("0F965BC2C949B91938787D5973C94856C", 16);
        BigInteger publicKey_bi = this.g.modPow(this.privateKey, this.p);
        try
        {
          com.WacomGSS.STU.Protocol.PublicKey publicKey = new com.WacomGSS.STU.Protocol.PublicKey(publicKey_bi.toByteArray());
          return publicKey;
        } 
        catch (Exception e)
        {
        }
        return null;
      }

      @Override
      public void computeSharedKey(com.WacomGSS.STU.Protocol.PublicKey devicePublicKey)
      {
        BigInteger devicePublicKey_bi = new BigInteger(1, devicePublicKey.getValue());
        BigInteger sharedKey = devicePublicKey_bi.modPow(this.privateKey, this.p);

        byte[] array = sharedKey.toByteArray();
        if (array[0] == 0)
        {
          byte[] tmp = new byte[array.length - 1];
          System.arraycopy(array, 1, tmp, 0, tmp.length);
          array = tmp;
        }

        try
        {
          Key aesKey = new SecretKeySpec(array, "AES");
          this.aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
          aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
          return;
        }
        catch (Exception e)
        {
        }
        this.aesCipher = null;
      }

      @Override
      public byte[] decrypt(byte[] data)
      {
        try
        {
          byte[] decryptedData = this.aesCipher.doFinal(data);
          return decryptedData;
        }
        catch (Exception e)
        {
        }
        return null;
      }
    }

    static class MyEncryptionHandler2 implements Tablet.IEncryptionHandler2
    {
      private Future<KeyPair> futureKeyPair;
      private Cipher  aesCipher;

      public MyEncryptionHandler2(Future<KeyPair> keyPair)
      {
        futureKeyPair = keyPair;
      }

      @Override
      public void reset()
      {
        clearKeys();
      }

      public void clearKeys()
      {
        this.aesCipher = null;
      }

      @Override
      public SymmetricKeyType getSymmetricKeyType()
      {
        return SymmetricKeyType.AES128;
      }

      @Override
      public AsymmetricPaddingType getAsymmetricPaddingType()
      {
        return AsymmetricPaddingType.PKCS1;
      }

      @Override
      public AsymmetricKeyType getAsymmetricKeyType()
      {
        return AsymmetricKeyType.RSA2048;
      }

      private int rsaKeySize()
      {
        switch (this.getAsymmetricKeyType())
        {
          case RSA1024: return 1024;
          case RSA1536: return 1536;
          case RSA2048: return 2048;
        }
        return 0;
      }

      private int aesKeySize()
      {
        switch (this.getSymmetricKeyType())
        {
          case AES128: return 128;
          case AES192: return 192;
          case AES256: return 256;
        }
        return 0;
      }

      private KeyPair ensureKeyPair()
      {
        try
        {
          return futureKeyPair.get();
        }
        catch (Exception e)
        {
          System.err.println("Error retrieving keyPair: " + e.getMessage());
        }
        return null;
      }

      @Override
      public byte[] getPublicExponent()
      {
        KeyPair keyPair = this.ensureKeyPair();
        byte[] ret = ((RSAPublicKey)keyPair.getPublic()).getPublicExponent().toByteArray();
        return ret;
      }

      @Override
      public byte[] generatePublicKey()
      {
        KeyPair keyPair = this.ensureKeyPair();
        byte[] modulus = ((RSAPublicKey)keyPair.getPublic()).getModulus().toByteArray();
        byte[] ret = new byte[rsaKeySize()/8];
        System.arraycopy(modulus, modulus.length-ret.length, ret, 0, ret.length);
        return ret;
      }

      @Override
      public void computeSessionKey(byte[] data)
      {
        KeyPair keyPair = this.ensureKeyPair();
        byte[] plaintext = null;
        try
        {
          Cipher rsaCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
          rsaCipher.init(Cipher.DECRYPT_MODE, keyPair.getPrivate());
          plaintext = rsaCipher.doFinal(data);
        }
        catch (Exception e)
        {
        }

        int keySizeBytes = this.aesKeySize()/8;
        if (plaintext.length != keySizeBytes)
        {
          byte[] k2 = new byte[keySizeBytes];
          if (plaintext.length > keySizeBytes)
            System.arraycopy(plaintext, plaintext.length-keySizeBytes, k2, 0, k2.length);
          else
            System.arraycopy(plaintext, 0, k2, 1, keySizeBytes - 1);
          plaintext = k2;
        }

        Key aesKey = new SecretKeySpec(plaintext, "AES");
        try
        {
          this.aesCipher = Cipher.getInstance("AES/ECB/NoPadding");
          this.aesCipher.init(Cipher.DECRYPT_MODE, aesKey);
          return;
        }
        catch (Exception e)
        {
        }
        this.aesCipher = null;
      }

      @Override
      public byte[] decrypt(byte[] data)
      {
        try
        {
          byte[] decryptedData = this.aesCipher.doFinal(data);
          return decryptedData;
        }
        catch (Exception e)
        {
        }
        return null;
      }
    }

    private Tablet        tablet;
    private Capability    capability;
    private Information   information;
    private String signerName;
    private String signReason;

    private static class Button
    {
      java.awt.Rectangle bounds;
      String             text;
      ActionListener     click;

      void performClick()
      {
        click.actionPerformed(null);
      }
    }

    private int isDown;
    private List<PenData> penData;
    private Button[] btns;
    private JPanel        panel;
    private boolean useSigMode;
    private BufferedImage bitmap;
    private EncodingMode encodingMode;
    private byte[] bitmapData;
    private boolean encrypted = false;
    private boolean isConfirmed = false;

    private Point2D.Float tabletToClient(PenData penData)
    {
      return new Point2D.Float( (float)penData.getX() * this.panel.getWidth()  / this.capability.getTabletMaxX(), 
                                (float)penData.getY() * this.panel.getHeight() / this.capability.getTabletMaxY() );
    }

    private Point2D.Float tabletToScreen(PenData penData)
    {
      return new Point2D.Float( (float)penData.getX() * this.capability.getScreenWidth() / this.capability.getTabletMaxX(), 
                                (float)penData.getY() * this.capability.getScreenHeight() / this.capability.getTabletMaxY() );
    }

    private Point clientToScreen(Point pt)
    {
      return new Point( Math.round( (float)pt.getX() * this.capability.getScreenWidth() / this.panel.getWidth() ), 
                        Math.round( (float)pt.getY() * this.capability.getScreenHeight() / this.panel.getHeight() ) );
    }

    private void pressOkButton() throws STUException
    {
      this.isConfirmed = true;
      try { this.tablet.setClearScreen(); } catch(Exception e) {}
      this.setVisible(false);
    }

    private void pressClearButton() throws STUException
    {
      clearScreen();
    }

    private void pressCancelButton() throws STUException
    {
      this.isConfirmed = false;
      this.setVisible(false);
      this.penData = null;
    }

    private void clearScreen() throws STUException
    {
      if (!this.useSigMode)
      {
        this.tablet.writeImage(this.encodingMode, this.bitmapData);
      }
      this.penData.clear();
      this.isDown = 0;
      this.panel.repaint();
    }

    public void dispose()
    {
      if (this.tablet != null)
      {
        try
        {
          this.tablet.setInkingMode(InkingMode.Off);
          if (encrypted) {
            this.tablet.endCapture();
            encrypted = false;
          }
          this.tablet.setOperationMode(OperationMode.initializeNormal());
          this.tablet.setClearScreen();
        }
        catch (Throwable t)
        {
        }
        this.tablet.disconnect();
        this.tablet = null;
      }
      super.dispose();
    }

    private void drawCenteredString(Graphics2D gfx, String text, int x, int y, int width, int height)
    {
      FontMetrics fm   = gfx.getFontMetrics(gfx.getFont());
      int textHeight   = fm.getHeight();
      int textWidth    = fm.stringWidth(text);
      int textX = x + (width  - textWidth) / 2;
      int textY = y + (height - textHeight) / 2 + fm.getAscent();
      gfx.drawString(text, textX, textY);
    }

    private void drawInk(Graphics2D gfx, PenData pd0, PenData pd1)
    {
      gfx.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      gfx.setColor(new Color(0,0,64,255));
      gfx.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));

      Point2D.Float pt0 = tabletToClient(pd0);
      Point2D.Float pt1 = tabletToClient(pd1);
      Shape l = new Line2D.Float(pt0, pt1);
      gfx.draw(l);
    }

    private void drawInk(Graphics2D gfx)
    {
      PenData[] pd = new PenData[0];
      pd = this.penData.toArray(pd);
      for (int i = 1; i < pd.length; ++i)
      {
        if (pd[i-1].getSw() != 0 && pd[i].getSw() != 0)
        {
          drawInk(gfx, pd[i-1], pd[i]);
        }
      }
    }

    public SignatureDialog(Frame frame, UsbDevice usbDevice, boolean useSigMode, Future<KeyPair> keyPair, String signerName, String signReason) throws STUException
    {
      super(frame, true);
      this.signerName = signerName;
      this.signReason = signReason;
      this.setTitle("Assinar no Tablet Wacom");
      this.setLocation(new Point(0, 0));
      if (frame != null) {
        this.setLocationRelativeTo(frame);
      } else {
        this.setLocation(100, 100);
      }
      
      this.panel = new JPanel()
        {
          @Override
          public void paintComponent(Graphics gfx)
          {
            super.paintComponent(gfx);
            if (bitmap != null)
            {
               Image rescaled = bitmap.getScaledInstance(panel.getWidth(),panel.getHeight(), Image.SCALE_SMOOTH);
               gfx.drawImage(rescaled, 0, 0, null);
               drawInk((Graphics2D)gfx);
            }
          }
        };

      this.panel.addMouseListener(new MouseAdapter()
        { 
          public void mouseClicked(MouseEvent e)
          {
            Point pt = clientToScreen(e.getPoint());
            for (Button btn : SignatureDialog.this.btns)
            {
              if (btn.bounds.contains(pt))
              {
                btn.performClick();
                break;
              }
            }
          }
        });

      this.penData = new ArrayList<PenData>();

      try
      {
        this.tablet = new Tablet();
        this.tablet.setEncryptionHandler(new MyEncryptionHandler());
        this.tablet.setEncryptionHandler2(new MyEncryptionHandler2(keyPair));
        
        int e = tablet.usbConnect(usbDevice, true);
        if (e == 0)
        {
          this.capability = tablet.getCapability();
          this.information = tablet.getInformation();
        }
        else
        {
          throw new RuntimeException("Falha ao ligar ao tablet USB, erro " + e);
        }

        if (useSigMode && !tablet.isSupported(ReportId.OperationMode))
        {
          useSigMode = false;
        }
        this.useSigMode = useSigMode;

        int screenResolution = this.getToolkit().getScreenResolution();
        Dimension d = new Dimension(this.capability.getTabletMaxX()*screenResolution/2540, this.capability.getTabletMaxY()*screenResolution/2540);
        this.panel.setPreferredSize(d);
        this.setLayout(new BorderLayout());
        this.setResizable(false);
        this.add(this.panel);
        this.pack();

        this.btns = new Button[3];
        this.btns[0] = new Button();
        this.btns[1] = new Button();
        this.btns[2] = new Button();

        if (useSigMode)
        {
          btns[0].bounds = new java.awt.Rectangle(  0, 431, 265, 48);
          btns[1].bounds = new java.awt.Rectangle(266, 431, 265, 48);
          btns[2].bounds = new java.awt.Rectangle(532, 431, 265, 48);
        }
        else if (this.tablet.getProductId() != UsbDevice.ProductId_300)
        {
          int w2 = this.capability.getScreenWidth() / 3;
          int w3 = this.capability.getScreenWidth() / 3;
          int w1 = this.capability.getScreenWidth() - w2 - w3;
          int y = this.capability.getScreenHeight() * 6 / 7;
          int h = this.capability.getScreenHeight() - y;

          btns[0].bounds = new java.awt.Rectangle(0, y, w1, h);
          btns[1].bounds = new java.awt.Rectangle(w1, y, w2, h);
          btns[2].bounds = new java.awt.Rectangle(w1 + w2, y, w3, h);
        }
        else
        {
          int x = this.capability.getScreenWidth() * 3 / 4;
          int w = this.capability.getScreenWidth() - x;
          int h2 = this.capability.getScreenHeight() / 3;
          int h3 = this.capability.getScreenHeight() / 3;
          int h1 = this.capability.getScreenHeight() - h2 - h3;

          btns[0].bounds = new java.awt.Rectangle(x, 0, w, h1);
          btns[1].bounds = new java.awt.Rectangle(x, h1, w, h2);
          btns[2].bounds = new java.awt.Rectangle(x, h1 + h2, w, h3);
        }

        btns[0].text = "Limpiar";
        btns[0].click = new ActionListener() 
          {
            public void actionPerformed(ActionEvent evt)
            {
              try { pressClearButton(); } catch (STUException e) {}
            }
          };

        btns[1].text = "Cancelar";
        btns[1].click = new ActionListener() 
          {
            public void actionPerformed(ActionEvent evt)
            {
              try { pressCancelButton(); } catch (STUException e) {}
            }
          };

        btns[2].text = "Aceptar";
        btns[2].click = new ActionListener() 
          {
            public void actionPerformed(ActionEvent evt)
            {
              try { pressOkButton(); } catch (STUException e) {}
            }
          };

        byte encodingFlag = ProtocolHelper.simulateEncodingFlag(this.tablet.getProductId(), this.capability.getEncodingFlag());
        if ((encodingFlag & EncodingFlag.EncodingFlag_24bit) != 0)
        {
          this.encodingMode = this.tablet.supportsWrite() ? EncodingMode.EncodingMode_24bit_Bulk : EncodingMode.EncodingMode_24bit;
        }
        else if ((encodingFlag & EncodingFlag.EncodingFlag_16bit) != 0)
        {
          this.encodingMode = this.tablet.supportsWrite() ? EncodingMode.EncodingMode_16bit_Bulk : EncodingMode.EncodingMode_16bit;
        }
        else
        {
          this.encodingMode = EncodingMode.EncodingMode_1bit;
        }

        if (useSigMode && !initializeSigMode())
        {
          useSigMode = false;
        }

        if (!useSigMode)
        {
          Color btnColor = (this.encodingMode == EncodingMode.EncodingMode_1bit) ? Color.WHITE : Color.LIGHT_GRAY;
          this.bitmap = createScreenImage(new Color[] { btnColor, btnColor, btnColor}, Color.BLACK, null);
          this.bitmapData = ProtocolHelper.flatten(this.bitmap, this.bitmap.getWidth(), this.bitmap.getHeight(), encodingMode);
        }

        this.tablet.addTabletHandler(this);
        clearScreen();

        if (ProtocolHelper.supportsEncryption(tablet.getProtocol())||tablet.isSupported(ReportId.EncryptionStatus))
        {
          this.tablet.startCapture(0xc0ffee);
          encrypted = true;
        }

        this.tablet.setInkingMode(InkingMode.On);
      }
      catch (Throwable t)
      {
        if (this.tablet != null)
        {
          this.tablet.disconnect();
          this.tablet = null;
        }
        throw t;
      }
    }

    public void onGetReportException(STUException e)
    {
      System.err.println("Erro na leitura do tablet: " + e.getMessage());
      this.tablet.disconnect();
      this.tablet = null;
      this.penData = null;
      this.setVisible(false);
    }

    public void onUnhandledReportData(byte[] data) {}

    public void onPenData(PenData penData)
    {
      Point2D pt = tabletToScreen(penData);
      int btn = 0;
      for (int i = 0; i < this.btns.length; ++i)
      {
        if (this.btns[i].bounds.contains(pt))
        {
          btn = i+1;
          break;
        }
      }

      boolean isDown = (penData.getSw() != 0);

      if (isDown)
      {
        if (this.isDown == 0)
        {
          if (btn > 0)
          {
            this.isDown = btn; 
          }
          else
          {
            this.isDown = -1;
          }
        }
        else
        {
          if (!this.penData.isEmpty() && this.isDown == -1)
          {
            Graphics2D gfx = (Graphics2D)this.panel.getGraphics();
            drawInk(gfx, this.penData.get(this.penData.size()-1), penData);
            gfx.dispose();
          }
        }

        if (this.isDown == -1)
          this.penData.add(penData);
      }
      else
      {
        if (this.isDown != 0)
        {
          if (btn > 0)
          {
            if (btn == this.isDown && !this.useSigMode)
            {
              this.btns[btn - 1].performClick();
            }
          }
          this.isDown = 0;
        }
        if (!this.penData.isEmpty())
          this.penData.add(penData);
      }
    }

    public void onPenDataOption(PenDataOption penDataOption)
    {
      onPenData(penDataOption);
    }

    public void onPenDataEncrypted(PenDataEncrypted penDataEncrypted)
    {
      onPenData(penDataEncrypted.getPenData1());
      onPenData(penDataEncrypted.getPenData2());
    }

    public void onPenDataEncryptedOption(PenDataEncryptedOption penDataEncryptedOption)
    {
      onPenData(penDataEncryptedOption.getPenDataOption1());
      onPenData(penDataEncryptedOption.getPenDataOption2());
    }

    public void onPenDataTimeCountSequence(PenDataTimeCountSequence penDataTimeCountSequence)
    {
      onPenData(penDataTimeCountSequence);
    }

    public void onPenDataTimeCountSequenceEncrypted(PenDataTimeCountSequenceEncrypted penDataTimeCountSequenceEncrypted)
    {
      onPenData(penDataTimeCountSequenceEncrypted);
    }

    public void onEncryptionStatus(EncryptionStatus encryptionStatus) {}
    public void onDevicePublicKey(DevicePublicKey devicePublicKey) {}
    public void onEventDataPinPad(EventDataPinPad eventData) {}
    public void onEventDataKeyPad(EventDataKeyPad eventData) {}
    public void onEventDataSignature(EventDataSignature eventData)
    {
      onSignatureEvent(eventData.getKeyValue());
    }
    public void onEventDataPinPadEncrypted(EventDataPinPadEncrypted eventData) {}
    public void onEventDataKeyPadEncrypted(EventDataKeyPadEncrypted eventData) {}
    public void onEventDataSignatureEncrypted(EventDataSignatureEncrypted eventData)
    {   
      onSignatureEvent(eventData.getKeyValue());
    }

    private void onSignatureEvent(byte keyValue)
    {
      try
      {
        switch (keyValue)
        {
          case (byte)0: pressCancelButton(); break;
          case (byte)1: pressOkButton(); break;
          case (byte)2: pressClearButton(); break;
        }
      }
      catch (Exception ex) {}
    }

    public PenData[] getPenData()
    {
      if (this.penData != null)
      {
        PenData[] arrayPenData = new PenData[0];
        return this.penData.toArray(arrayPenData);
      }
      return null;
    }

    public Information getInformation() { return this.information; }
    public Capability getCapability() { return this.capability; }

    private static final byte sigScreenImageNum = (byte)2;

    private void checkSigModeImage(boolean pushed, byte[] imageData) throws STUException, java.security.NoSuchAlgorithmException
    {
      boolean sigKeyEnabled[] = { true, true, true };
      RomStartImageData romStartImageData = RomStartImageData.initializeSignature(this.encodingMode, pushed, sigScreenImageNum, sigKeyEnabled);
      this.tablet.setRomImageHash(OperationModeType.Signature, pushed, sigScreenImageNum);
      RomImageHash romImgHash = tablet.getRomImageHash();

      boolean writeImage = true;
      if (romImgHash.getResult() == 0)
      {
        java.security.MessageDigest md = java.security.MessageDigest.getInstance("MD5");
        byte[] hash = md.digest(imageData);
        if (Arrays.equals(hash, romImgHash.getHash()))
        {
          writeImage = false;
        }
      }
      if (writeImage)
      {
        tablet.writeRomImage(romStartImageData, imageData);
      }
    }

    private BufferedImage createScreenImage(Color[] btnColors, Color txtColor, byte[] btnOrder)
    {
      BufferedImage image = new BufferedImage(this.capability.getScreenWidth(), this.capability.getScreenHeight(), BufferedImage.TYPE_INT_RGB);
      Graphics2D gfx = image.createGraphics();
      gfx.setColor(Color.WHITE);
      gfx.fillRect(0, 0, image.getWidth(), image.getHeight());

      double fontSize = (this.btns[0].bounds.getHeight() / 2.0);
      gfx.setFont(new Font("Arial", Font.PLAIN, (int)fontSize));

      for (int i = 0; i < this.btns.length; ++i)
      {
        Button              btn = this.btns[i];
        java.awt.Rectangle  bounds = this.btns[(btnOrder == null) ? i : btnOrder[i]].bounds;

        if (this.encodingMode != EncodingMode.EncodingMode_1bit)
        {
          gfx.setColor(btnColors[i]);
          gfx.fillRect((int)bounds.getX(), (int)bounds.getY(), (int)bounds.getWidth(), (int)bounds.getHeight());
        }
        gfx.setColor(txtColor);
        gfx.drawRect((int)bounds.getX(), (int)bounds.getY(), (int)bounds.getWidth(), (int)bounds.getHeight());
        drawCenteredString(gfx, btn.text, (int)bounds.getX(), (int)bounds.getY(), (int)bounds.getWidth(), (int)bounds.getHeight());
      }

      gfx.setColor(txtColor);
      gfx.setFont(new Font("Arial", Font.BOLD, 24));
      int yPos = 35;
      if (this.signReason != null && !this.signReason.isEmpty()) {
          String[] lines = this.signReason.split("\\n");
          for (String line : lines) {
              gfx.drawString(line, 10, yPos);
              yPos += 35;
          }
      }
      if (this.signerName != null && !this.signerName.isEmpty()) {
          yPos += 5;
          gfx.drawString("Firmante: " + this.signerName, 10, yPos);
          yPos += 30;
      }
      gfx.drawLine(10, yPos - 15, this.capability.getScreenWidth() - 10, yPos - 15);

      gfx.dispose();
      return image;
    }

    private boolean initializeSigMode()
    {
      try 
      {
        byte[]  btnOrder = { (byte)2, (byte)0, (byte)1 };
        Color[] btnsUpColors = new Color[] { new Color(0, 96, 255), Color.RED, Color.GREEN.darker() };
        Color[] btnsDownColors = new Color[] { btnsUpColors[0].darker(), btnsUpColors[1].darker(), btnsUpColors[2].darker() };
        byte[]  bitmapData;

        BufferedImage btnsUp  = createScreenImage(btnsUpColors, Color.BLACK, null);
        bitmapData = ProtocolHelper.flatten(btnsUp, btnsUp.getWidth(), btnsUp.getHeight(), encodingMode);
        checkSigModeImage(false, bitmapData);

        BufferedImage btnsPushed = createScreenImage(btnsDownColors, Color.WHITE, null);
        bitmapData = ProtocolHelper.flatten(btnsPushed, btnsPushed.getWidth(), btnsPushed.getHeight(), encodingMode);
        checkSigModeImage(true, bitmapData);

        OperationMode_Signature sigMode = new OperationMode_Signature(sigScreenImageNum, btnOrder, (byte)0, (byte)0 );
        this.tablet.setOperationMode(OperationMode.initializeSignature(sigMode));
        this.bitmap = createScreenImage(btnsUpColors, Color.BLACK, null);
        return true;
      }
      catch (Exception ex)
      {
        return false;
      }
    }
  }

  private static Point2D.Float tabletToClient(PenData penData, Capability capability, int width, int height)
  {
      return new Point2D.Float((float) penData.getX() * width / capability.getTabletMaxX(),
                (float) penData.getY() * height / capability.getTabletMaxY());
  }

  private static BufferedImage createImage(PenData[] penData, Capability capability, int width, int height) 
  {
      BufferedImage bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
      Graphics2D g = (Graphics2D) bi.getGraphics();
      g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      g.setColor(Color.WHITE);
      g.fillRect(0, 0, bi.getWidth(), bi.getHeight());
      g.setColor(new Color(0, 0, 64, 255));        
      g.setStroke(new BasicStroke(3, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));                        
      
      for (int i = 1; i < penData.length; i++)
      {            
          PenData p1 = penData[i];
          if (p1.getSw() != 0)
          {
              Point2D.Float pt1 = tabletToClient(penData[i - 1], capability, width, height);
              Point2D.Float pt2 = tabletToClient(penData[i], capability, width, height);
              Shape l = new Line2D.Float(pt1, pt2);
              g.draw(l);        
          }
      }
      return bi;
  }

  public void run()
  {
    this.keyPair = Executors.newSingleThreadExecutor().submit(()->
    {
      KeyPair keyPair = null;
      try
      {
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
        keyGen.initialize(2048);
        keyPair = keyGen.genKeyPair();
      }
      catch (Exception e)
      {
        System.err.println("Erro ao gerar par de chaves RSA: " + e.getMessage());
      }
      return keyPair;
    });

    try
    {
      com.WacomGSS.STU.UsbDevice[] usbDevices = UsbDevice.getUsbDevices();
      if (usbDevices != null && usbDevices.length > 0)
      {
        com.WacomGSS.STU.UsbDevice usbDevice = usbDevices[0];
        SignatureDialog signatureDialog = new SignatureDialog(null, usbDevice, false, this.keyPair, this.signerName, this.signReason);
        
        // Exibe o diálogo modal (bloqueia aqui até ser fechado)
        signatureDialog.setVisible(true);

        if (signatureDialog.isConfirmed)
        {
          PenData[] penData = signatureDialog.getPenData();
          if (penData != null && penData.length > 0)
          {
            // Gera a imagem final nas dimensões padrão do canvas do FS (450 x 200)
            BufferedImage signatureImage = createImage(penData, signatureDialog.getCapability(), 450, 200);
            ImageIO.write(signatureImage, "png", new File(this.imagePath));
            System.out.println("Assinatura salva com sucesso em " + this.imagePath);
          }
        }
        else
        {
          System.out.println("Assinatura cancelada pelo utilizador.");
        }
        signatureDialog.dispose();
      }
      else
      {
        System.err.println("Nenhum dispositivo Wacom STU conectado via USB.");
        System.exit(2);
      }
    }
    catch (Throwable e)
    {
      System.err.println("Erro durante a captura: " + e.getMessage());
      e.printStackTrace();
      System.exit(3);
    }
    System.exit(0);
  }

  public static void main(String[] args)
  {
    String name = args.length > 0 ? args[0] : "";
    String reason = args.length > 1 ? args[1] : "";
    String imgPath = args.length > 2 ? args[2] : "sig.png";
    WacomCapture capture = new WacomCapture(name, reason, imgPath);
    capture.run();
  }
}
