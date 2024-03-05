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
import android.text.Html;
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

import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

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

    private List<OrderLine> orderList;
    private int orderListSize;
    private String customerName;

    private String cashier;

    private String customerAddress;

    private String agentName;
    private String agentAddress;
    private String agentPhone;

    private String receiveDate;
    private String deliveryDate;
    private String customerPhone;
    private Double total;
    private String totalInRupiah;
    private String paymentMethod;
    private String paymentAmount;

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
                    orderList = response.body().data.orderLine;
                    orderListSize = orderList.size();
                    agentName = resource.data.agent.agentName;
                    agentAddress = resource.data.agent.agentAddress;
                    agentPhone = resource.data.agent.agentPhone;
                    receiptNumber = resource.data.name;
                    customerName = resource.data.customer.name;
                    cashier = resource.data.cashier;
                    customerAddress = resource.data.customer.address;
                    receiveDate = resource.data.receiveDate;
                    deliveryDate = resource.data.deliveryDate;
                    customerPhone = resource.data.customer.phone;
                    total = Double.valueOf(resource.data.amountTotal);
                    paymentMethod = resource.data.payment.get(0).paymentMethod;
                    paymentAmount = String.valueOf(resource.data.payment.get(0).amount);


                    Locale localeID = new Locale("in", "ID");
                    NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);
                    totalInRupiah = formatRupiah.format((double)total);
                    totalInRupiah = makeRpProperly(totalInRupiah, ' ',2);



                    Log.d(TAG,"[check value customer name] => " + customerName + "[check value receipt number] => " + receiptNumber +
                            "[check value cashier] => " + cashier + "[check value customer address] => " + customerAddress + "[check value agent address] => " + agentAddress +
                            "[check value agent name] => " + agentName + "[check value receive date] => " + receiveDate +
                            "[check value delivery date] => " + deliveryDate + "[check value customer phone] => " + customerPhone +
                            "[check value amount total] => " + totalInRupiah);

//                Integer total = resource.total;
//                Integer totalPages = resource.totalPages;
//                List<ReceiptModel.> datumList = resource.data;

//                displayResponse += text + " Page\n" + total + " Total\n" + totalPages + " Total Pages\n";

//                for (ReceiptModel.Datum datum : datumList) {
//                    displayResponse += datum.id + " " + datum.name + " " + datum.pantoneValue + " " + datum.year + "\n";
//                }

//                responseText.setText(displayResponse);
                    Log.d(TAG,"[response] => " + "[orderLine Size] => " + orderListSize
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


    public String makeRpProperly(String str, char ch, int position) {
        return str.substring(0, position) + ch + str.substring(position);
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

    private String decideWhichHtml(){
        Locale localeID = new Locale("in", "ID");
        NumberFormat formatRupiah = NumberFormat.getCurrencyInstance(localeID);

        for (int i = 0; i < orderListSize; i++) {

            Log.d("TAG", "[value order list] => " + orderList.get(i).name  + orderList.get(i).priceUnit
                    + orderList.get(i).orderedQty + orderList.get(i).orderedUom + orderList.get(i).discount);
        }

        if (orderListSize == 1){
            String orderName = orderList.get(0).name;
            Double orderPrice = orderList.get(0).priceUnit;
            Double orderQty = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit = orderPrice * orderQty;
            String sumPricePerUnitString;
            sumPricePerUnitString = formatRupiah.format(sumPricePerUnit);
            sumPricePerUnitString = makeRpProperly(sumPricePerUnitString, ' ',2);
            String pricePerUnit;
            pricePerUnit = formatRupiah.format((double) orderPrice);
            pricePerUnit = makeRpProperly(pricePerUnit, ' ',2);
            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]<u type='string'>" + orderName + "</u>[L]<u type='string'>" + sumPricePerUnitString + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty + "x" + pricePerUnit + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n"+
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 2) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ',2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ',2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ',2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ',2);
            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n"+
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 3) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ',2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ',2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ',2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ',2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(3).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ',2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ',2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n"+
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 4) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ',2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ',2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ',2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ',2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ',2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ',2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ',2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ',2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n"+
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n"+
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n"+
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 5) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ', 2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ', 2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ', 2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ', 2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ', 2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ', 2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ', 2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ', 2);

            String orderName5 = orderList.get(4).name;
            Double orderPrice5 = orderList.get(4).priceUnit;
            Double orderQty5 = orderList.get(4).orderedQty;
            Double discountPercentage5 = orderList.get(4).discount;
            double sumPricePerUnit5 = orderPrice5 * orderQty5;
            String sumPricePerUnitString5;
            sumPricePerUnitString5 = formatRupiah.format(sumPricePerUnit5);
            sumPricePerUnitString5 = makeRpProperly(sumPricePerUnitString5, ' ', 2);
            String pricePerUnit5;
            pricePerUnit5 = formatRupiah.format((double) orderPrice5);
            pricePerUnit5 = makeRpProperly(pricePerUnit5, ' ', 2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName5 + "</u>[L]<u type='string'>" + sumPricePerUnitString5 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty5 + "x" + pricePerUnit5 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage5 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n" +
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 6) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ', 2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ', 2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ', 2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ', 2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ', 2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ', 2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ', 2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ', 2);

            String orderName5 = orderList.get(4).name;
            Double orderPrice5 = orderList.get(4).priceUnit;
            Double orderQty5 = orderList.get(4).orderedQty;
            Double discountPercentage5 = orderList.get(4).discount;
            double sumPricePerUnit5 = orderPrice5 * orderQty5;
            String sumPricePerUnitString5;
            sumPricePerUnitString5 = formatRupiah.format(sumPricePerUnit5);
            sumPricePerUnitString5 = makeRpProperly(sumPricePerUnitString5, ' ', 2);
            String pricePerUnit5;
            pricePerUnit5 = formatRupiah.format((double) orderPrice5);
            pricePerUnit5 = makeRpProperly(pricePerUnit5, ' ', 2);

            String orderName6 = orderList.get(5).name;
            Double orderPrice6 = orderList.get(5).priceUnit;
            Double orderQty6 = orderList.get(5).orderedQty;
            Double discountPercentage6 = orderList.get(5).discount;
            double sumPricePerUnit6 = orderPrice6 * orderQty6;
            String sumPricePerUnitString6;
            sumPricePerUnitString6 = formatRupiah.format(sumPricePerUnit6);
            sumPricePerUnitString6 = makeRpProperly(sumPricePerUnitString6, ' ', 2);
            String pricePerUnit6;
            pricePerUnit6 = formatRupiah.format((double) orderPrice6);
            pricePerUnit6 = makeRpProperly(pricePerUnit6, ' ', 2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName5 + "</u>[L]<u type='string'>" + sumPricePerUnitString5 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty5 + "x" + pricePerUnit5 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage5 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName6 + "</u>[L]<u type='string'>" + sumPricePerUnitString6 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty6 + "x" + pricePerUnit6 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage6 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n" +
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 7) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ', 2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ', 2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ', 2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ', 2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ', 2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ', 2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ', 2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ', 2);

            String orderName5 = orderList.get(4).name;
            Double orderPrice5 = orderList.get(4).priceUnit;
            Double orderQty5 = orderList.get(4).orderedQty;
            Double discountPercentage5 = orderList.get(4).discount;
            double sumPricePerUnit5 = orderPrice5 * orderQty5;
            String sumPricePerUnitString5;
            sumPricePerUnitString5 = formatRupiah.format(sumPricePerUnit5);
            sumPricePerUnitString5 = makeRpProperly(sumPricePerUnitString5, ' ', 2);
            String pricePerUnit5;
            pricePerUnit5 = formatRupiah.format((double) orderPrice5);
            pricePerUnit5 = makeRpProperly(pricePerUnit5, ' ', 2);

            String orderName6 = orderList.get(5).name;
            Double orderPrice6 = orderList.get(5).priceUnit;
            Double orderQty6 = orderList.get(5).orderedQty;
            Double discountPercentage6 = orderList.get(5).discount;
            double sumPricePerUnit6 = orderPrice6 * orderQty6;
            String sumPricePerUnitString6;
            sumPricePerUnitString6 = formatRupiah.format(sumPricePerUnit6);
            sumPricePerUnitString6 = makeRpProperly(sumPricePerUnitString6, ' ', 2);
            String pricePerUnit6;
            pricePerUnit6 = formatRupiah.format((double) orderPrice6);
            pricePerUnit6 = makeRpProperly(pricePerUnit6, ' ', 2);

            String orderName7 = orderList.get(6).name;
            Double orderPrice7 = orderList.get(6).priceUnit;
            Double orderQty7 = orderList.get(6).orderedQty;
            Double discountPercentage7 = orderList.get(6).discount;
            double sumPricePerUnit7 = orderPrice7 * orderQty7;
            String sumPricePerUnitString7;
            sumPricePerUnitString7 = formatRupiah.format(sumPricePerUnit7);
            sumPricePerUnitString7 = makeRpProperly(sumPricePerUnitString7, ' ', 2);
            String pricePerUnit7;
            pricePerUnit7 = formatRupiah.format((double) orderPrice7);
            pricePerUnit7 = makeRpProperly(pricePerUnit7, ' ', 2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName5 + "</u>[L]<u type='string'>" + sumPricePerUnitString5 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty5 + "x" + pricePerUnit5 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage5 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName6 + "</u>[L]<u type='string'>" + sumPricePerUnitString6 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty6 + "x" + pricePerUnit6 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage6 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName7 + "</u>[L]<u type='string'>" + sumPricePerUnitString7 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty7 + "x" + pricePerUnit7 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage7 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n" +
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 8) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ', 2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ', 2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ', 2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ', 2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ', 2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ', 2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ', 2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ', 2);

            String orderName5 = orderList.get(4).name;
            Double orderPrice5 = orderList.get(4).priceUnit;
            Double orderQty5 = orderList.get(4).orderedQty;
            Double discountPercentage5 = orderList.get(4).discount;
            double sumPricePerUnit5 = orderPrice5 * orderQty5;
            String sumPricePerUnitString5;
            sumPricePerUnitString5 = formatRupiah.format(sumPricePerUnit5);
            sumPricePerUnitString5 = makeRpProperly(sumPricePerUnitString5, ' ', 2);
            String pricePerUnit5;
            pricePerUnit5 = formatRupiah.format((double) orderPrice5);
            pricePerUnit5 = makeRpProperly(pricePerUnit5, ' ', 2);

            String orderName6 = orderList.get(5).name;
            Double orderPrice6 = orderList.get(5).priceUnit;
            Double orderQty6 = orderList.get(5).orderedQty;
            Double discountPercentage6 = orderList.get(5).discount;
            double sumPricePerUnit6 = orderPrice6 * orderQty6;
            String sumPricePerUnitString6;
            sumPricePerUnitString6 = formatRupiah.format(sumPricePerUnit6);
            sumPricePerUnitString6 = makeRpProperly(sumPricePerUnitString6, ' ', 2);
            String pricePerUnit6;
            pricePerUnit6 = formatRupiah.format((double) orderPrice6);
            pricePerUnit6 = makeRpProperly(pricePerUnit6, ' ', 2);

            String orderName7 = orderList.get(6).name;
            Double orderPrice7 = orderList.get(6).priceUnit;
            Double orderQty7 = orderList.get(6).orderedQty;
            Double discountPercentage7 = orderList.get(6).discount;
            double sumPricePerUnit7 = orderPrice7 * orderQty7;
            String sumPricePerUnitString7;
            sumPricePerUnitString7 = formatRupiah.format(sumPricePerUnit7);
            sumPricePerUnitString7 = makeRpProperly(sumPricePerUnitString7, ' ', 2);
            String pricePerUnit7;
            pricePerUnit7 = formatRupiah.format((double) orderPrice7);
            pricePerUnit7 = makeRpProperly(pricePerUnit7, ' ', 2);

            String orderName8 = orderList.get(7).name;
            Double orderPrice8 = orderList.get(7).priceUnit;
            Double orderQty8 = orderList.get(7).orderedQty;
            Double discountPercentage8 = orderList.get(7).discount;
            double sumPricePerUnit8 = orderPrice8 * orderQty8;
            String sumPricePerUnitString8;
            sumPricePerUnitString8 = formatRupiah.format(sumPricePerUnit8);
            sumPricePerUnitString8 = makeRpProperly(sumPricePerUnitString8, ' ', 2);
            String pricePerUnit8;
            pricePerUnit8 = formatRupiah.format((double) orderPrice8);
            pricePerUnit8 = makeRpProperly(pricePerUnit8, ' ', 2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2+ "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName5 + "</u>[L]<u type='string'>" + sumPricePerUnitString5 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty5 + "x" + pricePerUnit5 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage5 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName6 + "</u>[L]<u type='string'>" + sumPricePerUnitString6 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty6 + "x" + pricePerUnit6 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage6 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName7 + "</u>[L]<u type='string'>" + sumPricePerUnitString7 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty7 + "x" + pricePerUnit7 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage7 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName8 + "</u>[L]<u type='string'>" + sumPricePerUnitString8 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty8 + "x" + pricePerUnit8 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage8 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n" +
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 9) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ', 2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ', 2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ', 2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ', 2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ', 2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ', 2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ', 2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ', 2);

            String orderName5 = orderList.get(4).name;
            Double orderPrice5 = orderList.get(4).priceUnit;
            Double orderQty5 = orderList.get(4).orderedQty;
            Double discountPercentage5 = orderList.get(4).discount;
            double sumPricePerUnit5 = orderPrice5 * orderQty5;
            String sumPricePerUnitString5;
            sumPricePerUnitString5 = formatRupiah.format(sumPricePerUnit5);
            sumPricePerUnitString5 = makeRpProperly(sumPricePerUnitString5, ' ', 2);
            String pricePerUnit5;
            pricePerUnit5 = formatRupiah.format((double) orderPrice5);
            pricePerUnit5 = makeRpProperly(pricePerUnit5, ' ', 2);

            String orderName6 = orderList.get(5).name;
            Double orderPrice6 = orderList.get(5).priceUnit;
            Double orderQty6 = orderList.get(5).orderedQty;
            Double discountPercentage6 = orderList.get(5).discount;
            double sumPricePerUnit6 = orderPrice6 * orderQty6;
            String sumPricePerUnitString6;
            sumPricePerUnitString6 = formatRupiah.format(sumPricePerUnit6);
            sumPricePerUnitString6 = makeRpProperly(sumPricePerUnitString6, ' ', 2);
            String pricePerUnit6;
            pricePerUnit6 = formatRupiah.format((double) orderPrice6);
            pricePerUnit6 = makeRpProperly(pricePerUnit6, ' ', 2);

            String orderName7 = orderList.get(6).name;
            Double orderPrice7 = orderList.get(6).priceUnit;
            Double orderQty7 = orderList.get(6).orderedQty;
            Double discountPercentage7 = orderList.get(6).discount;
            double sumPricePerUnit7 = orderPrice7 * orderQty7;
            String sumPricePerUnitString7;
            sumPricePerUnitString7 = formatRupiah.format(sumPricePerUnit7);
            sumPricePerUnitString7 = makeRpProperly(sumPricePerUnitString7, ' ', 2);
            String pricePerUnit7;
            pricePerUnit7 = formatRupiah.format((double) orderPrice7);
            pricePerUnit7 = makeRpProperly(pricePerUnit7, ' ', 2);

            String orderName8 = orderList.get(7).name;
            Double orderPrice8 = orderList.get(7).priceUnit;
            Double orderQty8 = orderList.get(7).orderedQty;
            Double discountPercentage8 = orderList.get(7).discount;
            double sumPricePerUnit8 = orderPrice8 * orderQty8;
            String sumPricePerUnitString8;
            sumPricePerUnitString8 = formatRupiah.format(sumPricePerUnit8);
            sumPricePerUnitString8 = makeRpProperly(sumPricePerUnitString8, ' ', 2);
            String pricePerUnit8;
            pricePerUnit8 = formatRupiah.format((double) orderPrice8);
            pricePerUnit8 = makeRpProperly(pricePerUnit8, ' ', 2);

            String orderName9 = orderList.get(8).name;
            Double orderPrice9 = orderList.get(8).priceUnit;
            Double orderQty9 = orderList.get(8).orderedQty;
            Double discountPercentage9 = orderList.get(8).discount;
            double sumPricePerUnit9 = orderPrice9 * orderQty9;
            String sumPricePerUnitString9;
            sumPricePerUnitString9 = formatRupiah.format(sumPricePerUnit9);
            sumPricePerUnitString9 = makeRpProperly(sumPricePerUnitString9, ' ', 2);
            String pricePerUnit9;
            pricePerUnit9 = formatRupiah.format((double) orderPrice9);
            pricePerUnit9 = makeRpProperly(pricePerUnit9, ' ', 2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2+ "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName5 + "</u>[L]<u type='string'>" + sumPricePerUnitString5 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty5 + "x" + pricePerUnit5 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage5 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName6 + "</u>[L]<u type='string'>" + sumPricePerUnitString6 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty6 + "x" + pricePerUnit6 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage6 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName7 + "</u>[L]<u type='string'>" + sumPricePerUnitString7 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty7 + "x" + pricePerUnit7 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage7 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName8 + "</u>[L]<u type='string'>" + sumPricePerUnitString8 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty8 + "x" + pricePerUnit8 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage8 + "</b>\n"+

                    "[L]<u type='string'>" + orderName9 + "</u>[L]<u type='string'>" + sumPricePerUnitString9 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty9 + "x" + pricePerUnit9 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage9 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n" +
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        else if (orderListSize == 10) {
            String orderName1 = orderList.get(0).name;
            Double orderPrice1 = orderList.get(0).priceUnit;
            Double orderQty1 = orderList.get(0).orderedQty;
            Double discountPercentage = orderList.get(0).discount;
            double sumPricePerUnit1 = orderPrice1 * orderQty1;
            String sumPricePerUnitString1;
            sumPricePerUnitString1 = formatRupiah.format(sumPricePerUnit1);
            sumPricePerUnitString1 = makeRpProperly(sumPricePerUnitString1, ' ', 2);
            String pricePerUnit1;
            pricePerUnit1 = formatRupiah.format((double) orderPrice1);
            pricePerUnit1 = makeRpProperly(pricePerUnit1, ' ', 2);

            String orderName2 = orderList.get(1).name;
            Double orderPrice2 = orderList.get(1).priceUnit;
            Double orderQty2 = orderList.get(1).orderedQty;
            Double discountPercentage2 = orderList.get(1).discount;
            double sumPricePerUnit2 = orderPrice2 * orderQty2;
            String sumPricePerUnitString2;
            sumPricePerUnitString2 = formatRupiah.format(sumPricePerUnit2);
            sumPricePerUnitString2 = makeRpProperly(sumPricePerUnitString2, ' ', 2);
            String pricePerUnit2;
            pricePerUnit2 = formatRupiah.format((double) orderPrice2);
            pricePerUnit2 = makeRpProperly(pricePerUnit2, ' ', 2);

            String orderName3 = orderList.get(2).name;
            Double orderPrice3 = orderList.get(2).priceUnit;
            Double orderQty3 = orderList.get(2).orderedQty;
            Double discountPercentage3 = orderList.get(2).discount;
            double sumPricePerUnit3 = orderPrice3 * orderQty3;
            String sumPricePerUnitString3;
            sumPricePerUnitString3 = formatRupiah.format(sumPricePerUnit3);
            sumPricePerUnitString3 = makeRpProperly(sumPricePerUnitString3, ' ', 2);
            String pricePerUnit3;
            pricePerUnit3 = formatRupiah.format((double) orderPrice3);
            pricePerUnit3 = makeRpProperly(pricePerUnit3, ' ', 2);

            String orderName4 = orderList.get(3).name;
            Double orderPrice4 = orderList.get(3).priceUnit;
            Double orderQty4 = orderList.get(3).orderedQty;
            Double discountPercentage4 = orderList.get(3).discount;
            double sumPricePerUnit4 = orderPrice4 * orderQty4;
            String sumPricePerUnitString4;
            sumPricePerUnitString4 = formatRupiah.format(sumPricePerUnit4);
            sumPricePerUnitString4 = makeRpProperly(sumPricePerUnitString4, ' ', 2);
            String pricePerUnit4;
            pricePerUnit4 = formatRupiah.format((double) orderPrice4);
            pricePerUnit4 = makeRpProperly(pricePerUnit4, ' ', 2);

            String orderName5 = orderList.get(4).name;
            Double orderPrice5 = orderList.get(4).priceUnit;
            Double orderQty5 = orderList.get(4).orderedQty;
            Double discountPercentage5 = orderList.get(4).discount;
            double sumPricePerUnit5 = orderPrice5 * orderQty5;
            String sumPricePerUnitString5;
            sumPricePerUnitString5 = formatRupiah.format(sumPricePerUnit5);
            sumPricePerUnitString5 = makeRpProperly(sumPricePerUnitString5, ' ', 2);
            String pricePerUnit5;
            pricePerUnit5 = formatRupiah.format((double) orderPrice5);
            pricePerUnit5 = makeRpProperly(pricePerUnit5, ' ', 2);

            String orderName6 = orderList.get(5).name;
            Double orderPrice6 = orderList.get(5).priceUnit;
            Double orderQty6 = orderList.get(5).orderedQty;
            Double discountPercentage6 = orderList.get(5).discount;
            double sumPricePerUnit6 = orderPrice6 * orderQty6;
            String sumPricePerUnitString6;
            sumPricePerUnitString6 = formatRupiah.format(sumPricePerUnit6);
            sumPricePerUnitString6 = makeRpProperly(sumPricePerUnitString6, ' ', 2);
            String pricePerUnit6;
            pricePerUnit6 = formatRupiah.format((double) orderPrice6);
            pricePerUnit6 = makeRpProperly(pricePerUnit6, ' ', 2);

            String orderName7 = orderList.get(6).name;
            Double orderPrice7 = orderList.get(6).priceUnit;
            Double orderQty7 = orderList.get(6).orderedQty;
            Double discountPercentage7 = orderList.get(6).discount;
            double sumPricePerUnit7 = orderPrice7 * orderQty7;
            String sumPricePerUnitString7;
            sumPricePerUnitString7 = formatRupiah.format(sumPricePerUnit7);
            sumPricePerUnitString7 = makeRpProperly(sumPricePerUnitString7, ' ', 2);
            String pricePerUnit7;
            pricePerUnit7 = formatRupiah.format((double) orderPrice7);
            pricePerUnit7 = makeRpProperly(pricePerUnit7, ' ', 2);

            String orderName8 = orderList.get(7).name;
            Double orderPrice8 = orderList.get(7).priceUnit;
            Double orderQty8 = orderList.get(7).orderedQty;
            Double discountPercentage8 = orderList.get(7).discount;
            double sumPricePerUnit8 = orderPrice8 * orderQty8;
            String sumPricePerUnitString8;
            sumPricePerUnitString8 = formatRupiah.format(sumPricePerUnit8);
            sumPricePerUnitString8 = makeRpProperly(sumPricePerUnitString8, ' ', 2);
            String pricePerUnit8;
            pricePerUnit8 = formatRupiah.format((double) orderPrice8);
            pricePerUnit8 = makeRpProperly(pricePerUnit8, ' ', 2);

            String orderName9 = orderList.get(8).name;
            Double orderPrice9 = orderList.get(8).priceUnit;
            Double orderQty9 = orderList.get(8).orderedQty;
            Double discountPercentage9 = orderList.get(8).discount;
            double sumPricePerUnit9 = orderPrice9 * orderQty9;
            String sumPricePerUnitString9;
            sumPricePerUnitString9 = formatRupiah.format(sumPricePerUnit9);
            sumPricePerUnitString9 = makeRpProperly(sumPricePerUnitString9, ' ', 2);
            String pricePerUnit9;
            pricePerUnit9 = formatRupiah.format((double) orderPrice9);
            pricePerUnit9 = makeRpProperly(pricePerUnit9, ' ', 2);

            String orderName10 = orderList.get(9).name;
            Double orderPrice10 = orderList.get(9).priceUnit;
            Double orderQty10 = orderList.get(9).orderedQty;
            Double discountPercentage10 = orderList.get(9).discount;
            double sumPricePerUnit10 = orderPrice10 * orderQty10;
            String sumPricePerUnitString10;
            sumPricePerUnitString10 = formatRupiah.format(sumPricePerUnit10);
            sumPricePerUnitString10 = makeRpProperly(sumPricePerUnitString10, ' ', 2);
            String pricePerUnit10;
            pricePerUnit10 = formatRupiah.format((double) orderPrice10);
            pricePerUnit10 = makeRpProperly(pricePerUnit10, ' ', 2);

            return "[L]\n" +
                    "[C]<b <font size='medium'>Your Receipt</b>\n" +
                    "[L]\n" +
                    "[C]<font size='medium'>" + receiptNumber + "</font>\n" +
                    "[L]\n" +
                    "[C]<u type='string'>" + agentAddress + "</u>\n" +
                    "[C]<u type='string'>" + agentPhone + "</u>\n" +
                    //"[C]<u type='double'>" + format.format(new Date()) + "</u>\n" +
                    "[C]================================\n" +
                    "[L]<b>Agen \t: </b>" + "[L]<u type='string'>" + agentName + "</u>\n" +
                    "[L]<b>Kasir \t: </b>" + "[L]<u type='string'>" + cashier + "</u>\n" +
                    "[L]<b>Tanggal Terima \t: </b>" + "[L]<u type='string'>" + receiveDate + "</u>\n" +
                    "[L]<b>Tanggal Selesai \t: </b>" + "[L]<u type='string'>" + deliveryDate + "</u>\n" +
                    "[L]<b>Detail Kustomer \t: </b>" + "[L]<u type='string'>" + customerName + "</u>\n" +
                    "[L]<b>Alamat \t: </b>" + "[L]<u type='string'>" + customerAddress + "</u>\n" +
                    "[L]<b>No. Telp \t: </b>" + "[L]<u type='string'>" + customerPhone + "</u>\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    // THIS IS THE LOOPING
                    "[L]<u type='string'>" + orderName1 + "</u>[L]<u type='string'>" + sumPricePerUnitString1 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty1 + "x" + pricePerUnit1 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2+ "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName2 + "</u>[L]<u type='string'>" + sumPricePerUnitString2 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty2 + "x" + pricePerUnit2 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage2 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName3 + "</u>[L]<u type='string'>" + sumPricePerUnitString3 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty3 + "x" + pricePerUnit3 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage3 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName4 + "</u>[L]<u type='string'>" + sumPricePerUnitString4 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty4 + "x" + pricePerUnit4 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage4 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName5 + "</u>[L]<u type='string'>" + sumPricePerUnitString5 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty5 + "x" + pricePerUnit5 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage5 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName6 + "</u>[L]<u type='string'>" + sumPricePerUnitString6 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty6 + "x" + pricePerUnit6 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage6 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName7 + "</u>[L]<u type='string'>" + sumPricePerUnitString7 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty7 + "x" + pricePerUnit7 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage7 + "</b>\n"+
                    "[L]\n" +

                    "[L]<u type='string'>" + orderName8 + "</u>[L]<u type='string'>" + sumPricePerUnitString8 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty8 + "x" + pricePerUnit8 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage8 + "</b>\n"+

                    "[L]<u type='string'>" + orderName9 + "</u>[L]<u type='string'>" + sumPricePerUnitString9 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty9 + "x" + pricePerUnit9 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage9 + "</b>\n"+

                    "[L]\n" +

                    "[L]<u type='string'>" + orderName10 + "</u>[L]<u type='string'>" + sumPricePerUnitString10 + "</u>\n" +
                    "[L]<b>Quantity</b>\n" +
                    "[L]<b type='string'>" + orderQty10 + "x" + pricePerUnit10 + " / Units" + "</b>\n" +
                    "[L]<b>Discount : </b>" + "[L]<b type='string'>" + discountPercentage10 + "</b>\n"+
                    // THIS IS THE LOOPING
                    "[L]\n" +
                    "[C]--------------------------------\n" +
                    "[L]<b>TOTAL   : </b>" + "[L]<u type='string'>" + totalInRupiah + "</u>\n" +
                    "[L]<b>PAYMENT : </b>" + "[L]<u type='string'>" + paymentMethod + "</u>\n" +
                    "[L]\n" +
                    "[C]================================\n" +
                    "[L]\n" +
                    "[L]\n" +

                    "[C]<b> Klarise Pusat </b>\n" +
                    "[C]Indonesia \n" +
                    "[C]<qrcode size='20'>https://web.klariselaundry.com/tnc</qrcode>\n";
        }
        return "null";
    }
    /**
     * Asynchronous printing
     */
    @SuppressLint("SimpleDateFormat")
    public AsyncEscPosPrinter getAsyncEscPosPrinter(DeviceConnection printerConnection) {
        SimpleDateFormat format = new SimpleDateFormat("'on' yyyy-MM-dd 'at' HH:mm:ss");
        AsyncEscPosPrinter printer = new AsyncEscPosPrinter(printerConnection, 203, 48f, 32);
        return printer.addTextToPrint(
                "[C]<img>" + PrinterTextParserImg.bitmapToHexadecimalString(printer, this.getApplicationContext().getResources().getDrawableForDensity(R.drawable.logo2, DisplayMetrics.DENSITY_MEDIUM)) + "</img>\n" +
                        decideWhichHtml()
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

