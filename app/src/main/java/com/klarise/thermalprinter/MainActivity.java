package com.klarise.thermalprinter;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.print.PrintAttributes;
import android.print.PrintDocumentAdapter;
import android.print.PrintManager;
import android.provider.OpenableColumns;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.klarise.escposprinter.connection.DeviceConnection;
import com.klarise.escposprinter.connection.bluetooth.BluetoothConnection;
import com.klarise.escposprinter.connection.bluetooth.BluetoothPrintersConnections;
import com.klarise.escposprinter.connection.tcp.TcpConnection;
import com.klarise.escposprinter.connection.usb.UsbConnection;
import com.klarise.escposprinter.connection.usb.UsbPrintersConnections;
import com.klarise.escposprinter.textparser.PrinterTextParserImg;
import com.klarise.thermalprinter.async.AsyncBluetoothEscPosPrint;
import com.klarise.thermalprinter.async.AsyncEscPosPrint;
import com.klarise.thermalprinter.async.AsyncEscPosPrinter;
import com.klarise.thermalprinter.async.AsyncTcpEscPosPrint;
import com.klarise.thermalprinter.async.AsyncUsbEscPosPrint;
import com.klarise.thermalprinter.model.OrderLine;
import com.klarise.thermalprinter.model.ReceiptModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    private String TAG = "MainActivity";
    private ActivityResultLauncher<Intent> pdfPickerLauncher;
    private Uri pdfUri;
    private String pdfName;
    private PrintDocumentAdapter printAdapter;

    private String filePath;

    private String[] split;

    private Uri uri;

    private APIInterface apiInterface;

    private String receiptNumberonUrl;
    private String receiptNumber;

    private int orderListSize;
    private String customerName;

    private String cashier;

    private String customerAddress;

    private String agentAddress;

    private String receiveDate;

    private String agentName;
    private String deliveryDate;
    private String customerPhone;
    private String total;
    private String paymentMethod;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Button button = (Button) this.findViewById(R.id.button_bluetooth_browse);
        button.setOnClickListener(view -> browseBluetoothDevice());
        button = (Button) findViewById(R.id.button_bluetooth);
        button.setOnClickListener(view -> printBluetooth());
        button = (Button) this.findViewById(R.id.button_usb);
        button.setOnClickListener(view -> printUsb());
        button = (Button) this.findViewById(R.id.button_tcp);
        button.setOnClickListener(view -> printTcp());
        button = (Button) this.findViewById(R.id.pick_pdf_button);
        button.setOnClickListener(view -> openPdfPicker());
        button = (Button) this.findViewById(R.id.pickprint_pdf_button);
        button.setOnClickListener(view -> pickPrint());
        TextView selectedPdfTextView = this.findViewById(R.id.selected_pdf_text_view);

        apiInterface = APIClient.getClient().create(APIInterface.class);


        // getting the data from our intent in our uri.
        uri = getIntent().getData();
        Log.d(TAG,"[check value uri deeplink] => " + uri);

        // checking if the uri is null or not.
        if (uri != null) {

            // if the uri is not null then we are getting
            // the path segments and storing it in list.
            List<String> parameters = uri.getPathSegments();

            // after that we are extracting string
            // from that parameters.
            receiptNumberonUrl = parameters.get(parameters.size() - 1);

            // on below line we are setting that string
            // to our text view which we got as params.
            selectedPdfTextView.setText(receiptNumberonUrl);

            /**
             GET Receipt Resources
             **/
            Call<ReceiptModel> call = apiInterface.getList(receiptNumberonUrl);
            call.enqueue(new Callback<ReceiptModel>() {
                @Override
                public void onResponse(Call<ReceiptModel> call, Response<ReceiptModel> response) {
                    Log.d("TAG", "[response success] => " + response);

                    String displayResponse = "";

                    ReceiptModel resource = response.body();
                    String text = resource.data.agenName;
                    List<OrderLine> orderList = response.body().data.orderLine;
                    int orderListSize = resource.data.orderLine.size();
                    String ordername = orderList.get(0).name;
                    String price = orderList.get(0).priceUnit;
                    String qty = orderList.get(0).orderedQty;
                    String uom = orderList.get(0).orderedUom;
                    for (int i = 0; i < orderListSize; i++) {
                        Log.d("TAG", "[value order list] => " + orderList.get(i).name  + orderList.get(i).priceUnit
                                + orderList.get(i).orderedQty + orderList.get(i).orderedUom);
                    }
                    agentName = resource.data.agenName;
                    receiptNumber = resource.data.name;
                    customerName = resource.data.customer.name;
                    cashier = resource.data.cashier;
                    customerAddress = resource.data.customer.address;
                    agentAddress = resource.data.agentAddress;
                    receiveDate = resource.data.receiveDate;
                    deliveryDate = resource.data.deliveryDate;
                    customerPhone = resource.data.customer.phone;
                    total = resource.data.amountTotal;
                    //paymentMethod = resource.data.payment.get(0);
                    Log.d(TAG,"[check value customer name] => " + customerName + "[check value receipt number] => " + receiptNumber +
                            "[check value cashier] => " + cashier + "[check value customer address] => " + customerAddress + "[check value agent address] => " + agentAddress +
                            "[check value agent name] => " + agentName + "[check value receive date] => " + receiveDate +
                            "[check value delivery date] => " + deliveryDate + "[check value customer phone] => " + customerPhone +
                            "[check value amount total] => " + total);



//                Integer total = resource.total;
//                Integer totalPages = resource.totalPages;
//                List<ReceiptModel.> datumList = resource.data;

//                displayResponse += text + " Page\n" + total + " Total\n" + totalPages + " Total Pages\n";

//                for (ReceiptModel.Datum datum : datumList) {
//                    displayResponse += datum.id + " " + datum.name + " " + datum.pantoneValue + " " + datum.year + "\n";
//                }

//                responseText.setText(displayResponse);
                    Log.d(TAG,"[response] => " + text + "[orderLine Size] => " + orderListSize
                            + "[orderList value] => " + orderList);
                }

                @Override
                public void onFailure(Call<ReceiptModel> call, Throwable t) {
                    call.cancel();
                    Log.d(TAG,"[error] => " + t);
                }
            });


        }


        pdfPickerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            pdfUri = data.getData();


                            pdfName = getFileName(pdfUri);
                            Log.d("MainActivity","[getPathFromURI value] => " + filePath);
                            selectedPdfTextView.setText(pdfName);
                            // Do something with the selected PDF file
                        }
                    } else {
                        Toast.makeText(MainActivity.this, "No PDF selected", Toast.LENGTH_SHORT).show();
                    }
                });


    }

    private void openPdfPicker() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.setType("application/pdf");
        pdfPickerLauncher.launch(intent);
    }

    private String getFileName(Uri uri) {
        String result = null;
        if (uri.getScheme().equals("content")) {
            try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                if (cursor != null && cursor.moveToFirst()) {
                    int columnIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                    if (columnIndex != -1) {
                        result = cursor.getString(columnIndex);
                    }
                }
            }
        }
        if (result == null) {
            result = uri.getLastPathSegment();
        }
        return result;
    }


    /*==============================================================================================
    ======================================BLUETOOTH PART============================================
    ==============================================================================================*/

    public interface OnBluetoothPermissionsGranted {
        void onPermissionsGranted();
    }

    public static final int PERMISSION_BLUETOOTH = 1;
    public static final int PERMISSION_BLUETOOTH_ADMIN = 2;
    public static final int PERMISSION_BLUETOOTH_CONNECT = 3;
    public static final int PERMISSION_BLUETOOTH_SCAN = 4;

    public OnBluetoothPermissionsGranted onBluetoothPermissionsGranted;

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            switch (requestCode) {
                case MainActivity.PERMISSION_BLUETOOTH:
                case MainActivity.PERMISSION_BLUETOOTH_ADMIN:
                case MainActivity.PERMISSION_BLUETOOTH_CONNECT:
                case MainActivity.PERMISSION_BLUETOOTH_SCAN:
                    this.checkBluetoothPermissions(this.onBluetoothPermissionsGranted);
                    break;
            }
        }
    }

    public void checkBluetoothPermissions(OnBluetoothPermissionsGranted onBluetoothPermissionsGranted) {
        this.onBluetoothPermissionsGranted = onBluetoothPermissionsGranted;
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH}, MainActivity.PERMISSION_BLUETOOTH);
        } else if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_ADMIN}, MainActivity.PERMISSION_BLUETOOTH_ADMIN);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_CONNECT}, MainActivity.PERMISSION_BLUETOOTH_CONNECT);
        } else if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S && ContextCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.BLUETOOTH_SCAN}, MainActivity.PERMISSION_BLUETOOTH_SCAN);
        } else {
            this.onBluetoothPermissionsGranted.onPermissionsGranted();
        }
    }

    private BluetoothConnection selectedDevice;

    public void browseBluetoothDevice() {
        this.checkBluetoothPermissions(() -> {
            final BluetoothConnection[] bluetoothDevicesList = (new BluetoothPrintersConnections()).getList();

            if (bluetoothDevicesList != null) {
                final String[] items = new String[bluetoothDevicesList.length + 1];
                items[0] = "Default printer";
                int i = 0;
                for (BluetoothConnection device : bluetoothDevicesList) {
                    if (ActivityCompat.checkSelfPermission(this, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                        // TODO: Consider calling
                        //    ActivityCompat#requestPermissions
                        // here to request the missing permissions, and then overriding
                        //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
                        //                                          int[] grantResults)
                        // to handle the case where the user grants the permission. See the documentation
                        // for ActivityCompat#requestPermissions for more details.
                        return;
                    }
                    items[++i] = device.getDevice().getName();
                }

                AlertDialog.Builder alertDialog = new AlertDialog.Builder(MainActivity.this);
                alertDialog.setTitle("Bluetooth printer selection");
                alertDialog.setItems(
                    items,
                    (dialogInterface, i1) -> {
                        int index = i1 - 1;
                        if (index == -1) {
                            selectedDevice = null;
                        } else {
                            selectedDevice = bluetoothDevicesList[index];
                        }
                        Button button = (Button) findViewById(R.id.button_bluetooth_browse);
                        button.setText(items[i1]);
                    }
                );

                AlertDialog alert = alertDialog.create();
                alert.setCanceledOnTouchOutside(false);
                alert.show();
            }
        });

    }

    public void printBluetooth() {
        this.checkBluetoothPermissions(() -> {
            new AsyncBluetoothEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                    }
                }
            )
                .execute(this.getAsyncEscPosPrinter(selectedDevice));
        });
    }

    /*==============================================================================================
    ===========================================USB PART=============================================
    ==============================================================================================*/

    private static final String ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION";
    private final BroadcastReceiver usbReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (MainActivity.ACTION_USB_PERMISSION.equals(action)) {
                synchronized (this) {
                    UsbManager usbManager = (UsbManager) getSystemService(Context.USB_SERVICE);
                    UsbDevice usbDevice = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        if (usbManager != null && usbDevice != null) {
                            new AsyncUsbEscPosPrint(
                                context,
                                new AsyncEscPosPrint.OnPrintFinished() {
                                    @Override
                                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                                        Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                                    }

                                    @Override
                                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                                        Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                                    }
                                }
                            )
                                .execute(getAsyncEscPosPrinter(new UsbConnection(usbManager, usbDevice)));
                        }
                    }
                }
            }
        }
    };

    public void printUsb() {
        UsbConnection usbConnection = UsbPrintersConnections.selectFirstConnected(this);
        UsbManager usbManager = (UsbManager) this.getSystemService(Context.USB_SERVICE);

        if (usbConnection == null || usbManager == null) {
            new AlertDialog.Builder(this)
                .setTitle("USB Connection")
                .setMessage("No USB printer found.")
                .show();
            return;
        }

        PendingIntent permissionIntent = PendingIntent.getBroadcast(
            this,
            0,
            new Intent(MainActivity.ACTION_USB_PERMISSION),
            android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S ? PendingIntent.FLAG_MUTABLE : 0
        );
        IntentFilter filter = new IntentFilter(MainActivity.ACTION_USB_PERMISSION);
        registerReceiver(this.usbReceiver, filter);
        usbManager.requestPermission(usbConnection.getDevice(), permissionIntent);
    }

    /*==============================================================================================
    =========================================TCP PART===============================================
    ==============================================================================================*/

    public void printTcp() {
        final EditText ipAddress = (EditText) this.findViewById(R.id.edittext_tcp_ip);
        final EditText portAddress = (EditText) this.findViewById(R.id.edittext_tcp_port);

        try {
            new AsyncTcpEscPosPrint(
                this,
                new AsyncEscPosPrint.OnPrintFinished() {
                    @Override
                    public void onError(AsyncEscPosPrinter asyncEscPosPrinter, int codeException) {
                        Log.e("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : An error occurred !");
                    }

                    @Override
                    public void onSuccess(AsyncEscPosPrinter asyncEscPosPrinter) {
                        Log.i("Async.OnPrintFinished", "AsyncEscPosPrint.OnPrintFinished : Print is finished !");
                    }
                }
            )
                .execute(
                    this.getAsyncEscPosPrinter(
                        new TcpConnection(
                            ipAddress.getText().toString(),
                            Integer.parseInt(portAddress.getText().toString())
                        )
                    )
                );
        } catch (NumberFormatException e) {
            new AlertDialog.Builder(this)
                .setTitle("Invalid TCP port address")
                .setMessage("Port field must be an integer.")
                .show();
            e.printStackTrace();
        }
    }

    /*==============================================================================================
    ===================================ESC/POS PRINTER PART=========================================
    ==============================================================================================*/

    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) {
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);
        return printer.addTextToPrint(
            "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logo2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n" +
                "[L]\n" +
                "[C]<b>Your Receipt</b>\n" +
                "[L]\n" +
                "[C]<u><font size='16'>" + receiptNumber + "</font></u>\n" +
                "[L]\n" +
                "[C]<u type='string'>" + agentAddress + "</u>\n" +
                //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                "[C]================================\n" +
                "[L]<b>Agen \t\t : </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                "[L]<b>Kasir \t\t : </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                "[L]<b>Tanggal Terima \t\t : </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                "[L]<b>Tanggal Selesai \t\t : </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                "[L]<b>Detail Kustomer \t\t : </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                "[L]    Alamat      :   " + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                "[L]    No. Telp    :   " + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                "[C]================================\n" +
                "[L]\n" +
                //"[L]<b> type='string'>" + orderName + "</b>\n" + "[R]<u type='string'>" + price + "</u>\n"
                "[L]  + Size : S\n" +
                "[L]\n" +
                "[L]<b>AWESOME HAT</b>[R]24.99€\n" +
                "[L]  + Size : 57/58\n" +
                "[L]\n" +
                "[C]--------------------------------\n" +
                "[L]<b>TOTAL \t\t : </b>" + "[L]<u type='string'>" + total + "</u>\n" +
                 //"[R]TOTAL PRICE :[R]34.98€\n" +
                //"[R]TAX :[R]4.23€\n" +
                "[L]\n" +
                "[C]================================\n" +
                "[L]\n" +
                //"[L]<u><font color='bg-black' size='tall'>Customer :</font></u>\n" +
                "[L]Syarat dan Ketentuan: \n" +
                "[L]PERHATIAN: \n" +
                "[L]1. Batas komplain 1x24j am setelah barang diterima.\n" +
                    "2. Kerusakan yang disebabkan oleh kelalaian pelanggan karena tidak menginfokan adapakaian yang potensi rusak karena bahan (menciut / robek / brudul) bukan menjadi tanggung jawab Klarise.\n" +
                    "3. Pencucian 1 wadah 1 mesin.\n" +
                    "4. Kelunturan yang disebabkan oleh kelalaian pelanggan jika tidak menginfokan ada pakaian potensi luntur bukan menjadi tanggung jawab Klarise.\n" +
                    "5. Kehilangan pakaian yang disebabkan karena kelalaian Klarise,akan diganti 3x harga pencucian.\n" +
                    "6. Garansi cuci ulang jika barang tidak bersih atau berbau apek(1x24 jam setelah barang diterima).\n" +
                    "7. Pembayaran lunas diawal.\n" +
                    "8. Barang yang hilang bukan menjadi tanggungjawab pihak Klarise jika barang tidak diambil lewat dari 7x24 jam serta tidak ada info ke pihak Klarise.\n" +
                    "9. Pihak Klarise tidak akan bertanggungjawab atas barang yang tertinggal di dalam pakaian.\n" +
                "[L]\n" +
                "[L]\n" +
                // "[L]Tel : +33801201456\n" +
                //"[C]<barcode type='ean13' height='10'>831254784551</barcode>\n" +
                "[C]<b> Klarise Pusat </b>\n" +
                "[C]Indonesia \n" +
                "[C]Feel free to email us if you need our help. \n"
                //"[C]<qrcode size='20'>https://klariselaundry.com/api/ereceipt/126</qrcode>\n"
        );
    }

    public void pickPrint(){
        PrintManager printManager= null;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            printManager = (PrintManager) this.getSystemService(Context.PRINT_SERVICE);
        }
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                Log.d("Main Activity","[value filePath] =>" + filePath);
                printAdapter = new PdfDocumentAdapter(this, filePath);
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                printManager.print("Document", printAdapter,new PrintAttributes.Builder().build());
            }
        }
    catch (Exception e) {
        Log.e("Main Activity", "[pickprint error] =>" + e);
    }
    }
}

