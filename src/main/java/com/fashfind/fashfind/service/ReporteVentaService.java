package com.fashfind.fashfind.service;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

import org.apache.poi.ss.usermodel.BorderStyle;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFFont;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import com.fashfind.fashfind.entity.Venta;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;

/**
 * Genera el reporte de ventas en PDF (OpenPDF) y Excel (Apache POI),
 * con los mismos indicadores mostrados en /ventas/reporte y los colores
 * de marca de FashFind (rosa #e91e8c, oscuro #1a1a2e).
 *
 * Solo recibe ventas activas: el filtrado por estado se hace en el
 * controlador antes de llamar a estos metodos.
 */
@Service
public class ReporteVentaService {

    private static final Color ROSA_AWT = new Color(0xE9, 0x1E, 0x8C);
    private static final Color ROSA_OSCURO_AWT = new Color(0xC2, 0x18, 0x5B);
    private static final Color OSCURO_AWT = new Color(0x1A, 0x1A, 0x2E);
    private static final Color ROSA_CLARO_AWT = new Color(0xFD, 0xF0, 0xF7);
    private static final Color GRIS_BORDE_AWT = new Color(0xEA, 0xEA, 0xEF);
    private static final byte[] ROSA_RGB = new byte[]{(byte) 0xE9, (byte) 0x1E, (byte) 0x8C};
    private static final byte[] ROSA_OSCURO_RGB = new byte[]{(byte) 0xC2, (byte) 0x18, (byte) 0x5B};
    private static final byte[] ROSA_CLARO_RGB = new byte[]{(byte) 0xFD, (byte) 0xF0, (byte) 0xF7};
    private static final byte[] OSCURO_RGB = new byte[]{(byte) 0x1A, (byte) 0x1A, (byte) 0x2E};

    private static final String[] COLUMNAS =
            {"#", "Fecha", "Hora", "Vendedor", "Metodo de Pago", "Unidades", "Total"};

    public byte[] generarPdf(List<Venta> ventas, long totalVentas, long unidadesVendidas,
                              long ingresosTotales, long ventasEfectivo, long ventasTransferencia, long ticketPromedio) {
        Document documento = new Document(PageSize.A4.rotate(), 28, 28, 90, 44);
        ByteArrayOutputStream salida = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(documento, salida);
            writer.setPageEvent(new EncabezadoPiePdf("Reporte de Ventas"));
            documento.open();

            Font fuenteEtiqueta = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);
            Font fuenteValor = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14, ROSA_OSCURO_AWT);
            Font fuenteEncabezado = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9, Color.WHITE);
            Font fuenteCelda = FontFactory.getFont(FontFactory.HELVETICA, 9, OSCURO_AWT);
            Font fuenteSeccion = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 11, OSCURO_AWT);

            documento.add(new Paragraph("Indicadores generales", fuenteSeccion));
            documento.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            String[] etiquetas = {"Ventas", "Unidades", "Efectivo", "Transferencia", "Ticket Promedio", "Ingresos"};
            String[] valores = {
                    String.valueOf(totalVentas),
                    String.valueOf(unidadesVendidas),
                    String.valueOf(ventasEfectivo),
                    String.valueOf(ventasTransferencia),
                    "$" + formatearMiles(ticketPromedio),
                    "$" + formatearMiles(ingresosTotales)
            };

            PdfPTable resumen = new PdfPTable(etiquetas.length);
            resumen.setWidthPercentage(100);
            resumen.setSpacingAfter(16);
            for (int i = 0; i < etiquetas.length; i++) {
                PdfPCell celda = new PdfPCell();
                celda.setBackgroundColor(ROSA_CLARO_AWT);
                celda.setBorder(Rectangle.BOTTOM);
                celda.setBorderColor(ROSA_AWT);
                celda.setBorderWidth(2f);
                celda.setPadding(10);
                celda.addElement(new Paragraph(etiquetas[i].toUpperCase(), fuenteEtiqueta));
                celda.addElement(new Paragraph(valores[i], fuenteValor));
                resumen.addCell(celda);
            }
            documento.add(resumen);

            documento.add(new Paragraph("Detalle de ventas activas", fuenteSeccion));
            documento.add(new Paragraph(" ", FontFactory.getFont(FontFactory.HELVETICA, 4)));

            PdfPTable tabla = new PdfPTable(COLUMNAS.length);
            tabla.setWidthPercentage(100);
            tabla.setWidths(new float[]{0.6f, 1.1f, 0.9f, 1.7f, 1.5f, 1f, 1.3f});
            tabla.setHeaderRows(1);
            for (int i = 0; i < COLUMNAS.length; i++) {
                boolean numerica = i == 5 || i == 6;
                PdfPCell celda = new PdfPCell(new Phrase(COLUMNAS[i], fuenteEncabezado));
                celda.setBackgroundColor(ROSA_OSCURO_AWT);
                celda.setPadding(7);
                celda.setHorizontalAlignment(numerica ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
                celda.setBorder(0);
                tabla.addCell(celda);
            }

            DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");
            boolean parImpar = false;
            for (Venta v : ventas) {
                Color fondoFila = parImpar ? ROSA_CLARO_AWT : Color.WHITE;
                agregarCelda(tabla, "#" + v.getIdVenta(), fuenteCelda, Element.ALIGN_LEFT, fondoFila);
                agregarCelda(tabla, v.getFechaVenta().format(formatoFecha), fuenteCelda, Element.ALIGN_LEFT, fondoFila);
                agregarCelda(tabla, v.getHora().format(formatoHora), fuenteCelda, Element.ALIGN_LEFT, fondoFila);
                agregarCelda(tabla, v.getUsuario().getNombres() + " " + v.getUsuario().getApellidos(), fuenteCelda, Element.ALIGN_LEFT, fondoFila);
                agregarCelda(tabla, v.getMetodoPago().toString(), fuenteCelda, Element.ALIGN_LEFT, fondoFila);
                agregarCelda(tabla, String.valueOf(v.getTotalUnidades()), fuenteCelda, Element.ALIGN_RIGHT, fondoFila);
                agregarCelda(tabla, "$" + formatearMiles(v.getCostoTotal()), fuenteCelda, Element.ALIGN_RIGHT, fondoFila);
                parImpar = !parImpar;
            }
            documento.add(tabla);
        } catch (Exception e) {
            throw new RuntimeException("No se pudo generar el PDF del reporte de ventas", e);
        } finally {
            if (documento.isOpen()) {
                documento.close();
            }
        }
        return salida.toByteArray();
    }

    public byte[] generarExcel(List<Venta> ventas, long totalVentas, long unidadesVendidas,
                                long ingresosTotales, long ventasEfectivo, long ventasTransferencia, long ticketPromedio) {
        try (XSSFWorkbook libro = new XSSFWorkbook(); ByteArrayOutputStream salida = new ByteArrayOutputStream()) {
            Sheet hoja = libro.createSheet("Ventas");
            hoja.setDisplayGridlines(false);

            XSSFCellStyle estiloBandaTitulo = libro.createCellStyle();
            estiloBandaTitulo.setFillForegroundColor(new XSSFColor(OSCURO_RGB, null));
            estiloBandaTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloBandaTitulo.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            XSSFFont fuenteTitulo = libro.createFont();
            fuenteTitulo.setBold(true);
            fuenteTitulo.setFontHeightInPoints((short) 16);
            fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
            estiloBandaTitulo.setFont(fuenteTitulo);

            XSSFCellStyle estiloBandaSub = libro.createCellStyle();
            estiloBandaSub.setFillForegroundColor(new XSSFColor(OSCURO_RGB, null));
            estiloBandaSub.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloBandaSub.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            XSSFFont fuenteSub = libro.createFont();
            fuenteSub.setColor(new XSSFColor(new byte[]{(byte) 0xCF, (byte) 0xCF, (byte) 0xDA}, null));
            fuenteSub.setFontHeightInPoints((short) 10);
            estiloBandaSub.setFont(fuenteSub);

            Row filaTitulo = hoja.createRow(0);
            filaTitulo.setHeightInPoints(28);
            filaTitulo.createCell(0).setCellValue("FashFind · Reporte de Ventas");
            filaTitulo.getCell(0).setCellStyle(estiloBandaTitulo);
            for (int i = 1; i < COLUMNAS.length; i++) {
                filaTitulo.createCell(i).setCellStyle(estiloBandaTitulo);
            }
            hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, COLUMNAS.length - 1));

            String fecha = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
            Row filaSub = hoja.createRow(1);
            filaSub.setHeightInPoints(18);
            filaSub.createCell(0).setCellValue("Generado el " + fecha + "  ·  Solo ventas activas");
            filaSub.getCell(0).setCellStyle(estiloBandaSub);
            for (int i = 1; i < COLUMNAS.length; i++) {
                filaSub.createCell(i).setCellStyle(estiloBandaSub);
            }
            hoja.addMergedRegion(new CellRangeAddress(1, 1, 0, COLUMNAS.length - 1));

            String[] etiquetasResumen = {"Ventas", "Unidades", "Efectivo", "Transferencia", "Ticket Promedio", "Ingresos"};
            long[] valoresResumen = {totalVentas, unidadesVendidas, ventasEfectivo, ventasTransferencia, ticketPromedio, ingresosTotales};

            XSSFCellStyle estiloTarjetaEtiqueta = libro.createCellStyle();
            estiloTarjetaEtiqueta.setFillForegroundColor(new XSSFColor(ROSA_CLARO_RGB, null));
            estiloTarjetaEtiqueta.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloTarjetaEtiqueta.setBorderTop(BorderStyle.THIN);
            estiloTarjetaEtiqueta.setBorderLeft(BorderStyle.THIN);
            estiloTarjetaEtiqueta.setBorderRight(BorderStyle.THIN);
            estiloTarjetaEtiqueta.setTopBorderColor(IndexedColors.WHITE.getIndex());
            estiloTarjetaEtiqueta.setLeftBorderColor(IndexedColors.WHITE.getIndex());
            estiloTarjetaEtiqueta.setRightBorderColor(IndexedColors.WHITE.getIndex());
            XSSFFont fuenteTarjetaEtiqueta = libro.createFont();
            fuenteTarjetaEtiqueta.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
            fuenteTarjetaEtiqueta.setFontHeightInPoints((short) 9);
            estiloTarjetaEtiqueta.setFont(fuenteTarjetaEtiqueta);

            XSSFCellStyle estiloTarjetaValor = libro.createCellStyle();
            estiloTarjetaValor.setFillForegroundColor(new XSSFColor(ROSA_CLARO_RGB, null));
            estiloTarjetaValor.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloTarjetaValor.setBorderBottom(BorderStyle.MEDIUM);
            estiloTarjetaValor.setBottomBorderColor(new XSSFColor(ROSA_RGB, null));
            estiloTarjetaValor.setBorderLeft(BorderStyle.THIN);
            estiloTarjetaValor.setBorderRight(BorderStyle.THIN);
            estiloTarjetaValor.setLeftBorderColor(IndexedColors.WHITE.getIndex());
            estiloTarjetaValor.setRightBorderColor(IndexedColors.WHITE.getIndex());
            XSSFFont fuenteTarjetaValor = libro.createFont();
            fuenteTarjetaValor.setBold(true);
            fuenteTarjetaValor.setFontHeightInPoints((short) 12);
            fuenteTarjetaValor.setColor(new XSSFColor(ROSA_OSCURO_RGB, null));
            estiloTarjetaValor.setFont(fuenteTarjetaValor);

            Row filaEtiquetasResumen = hoja.createRow(3);
            Row filaValoresResumen = hoja.createRow(4);
            filaValoresResumen.setHeightInPoints(20);
            for (int i = 0; i < etiquetasResumen.length; i++) {
                filaEtiquetasResumen.createCell(i).setCellValue(etiquetasResumen[i]);
                filaEtiquetasResumen.getCell(i).setCellStyle(estiloTarjetaEtiqueta);
                if (i == 4 || i == 5) {
                    filaValoresResumen.createCell(i).setCellValue("$" + formatearMiles(valoresResumen[i]));
                } else {
                    filaValoresResumen.createCell(i).setCellValue(valoresResumen[i]);
                }
                filaValoresResumen.getCell(i).setCellStyle(estiloTarjetaValor);
            }

            XSSFCellStyle estiloEncabezado = libro.createCellStyle();
            estiloEncabezado.setFillForegroundColor(new XSSFColor(ROSA_OSCURO_RGB, null));
            estiloEncabezado.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            estiloEncabezado.setAlignment(HorizontalAlignment.CENTER);
            estiloEncabezado.setVerticalAlignment(org.apache.poi.ss.usermodel.VerticalAlignment.CENTER);
            XSSFFont fuenteEncabezado = libro.createFont();
            fuenteEncabezado.setBold(true);
            fuenteEncabezado.setColor(IndexedColors.WHITE.getIndex());
            estiloEncabezado.setFont(fuenteEncabezado);

            XSSFCellStyle estiloFilaBlanca = estiloFilaDatos(libro, false, false);
            XSSFCellStyle estiloFilaBlancaNum = estiloFilaDatos(libro, false, true);
            XSSFCellStyle estiloFilaRosa = estiloFilaDatos(libro, true, false);
            XSSFCellStyle estiloFilaRosaNum = estiloFilaDatos(libro, true, true);

            int filaEncabezadoIdx = 6;
            Row filaEncabezado = hoja.createRow(filaEncabezadoIdx);
            filaEncabezado.setHeightInPoints(20);
            for (int i = 0; i < COLUMNAS.length; i++) {
                filaEncabezado.createCell(i).setCellValue(COLUMNAS[i]);
                filaEncabezado.getCell(i).setCellStyle(estiloEncabezado);
            }

            DateTimeFormatter formatoFecha = DateTimeFormatter.ofPattern("dd/MM/yyyy");
            DateTimeFormatter formatoHora = DateTimeFormatter.ofPattern("HH:mm");
            int fila = filaEncabezadoIdx + 1;
            boolean parImpar = false;
            for (Venta v : ventas) {
                Row r = hoja.createRow(fila++);
                XSSFCellStyle texto = parImpar ? estiloFilaRosa : estiloFilaBlanca;
                XSSFCellStyle numero = parImpar ? estiloFilaRosaNum : estiloFilaBlancaNum;

                celdaTexto(r, 0, "#" + v.getIdVenta(), texto);
                celdaTexto(r, 1, v.getFechaVenta().format(formatoFecha), texto);
                celdaTexto(r, 2, v.getHora().format(formatoHora), texto);
                celdaTexto(r, 3, v.getUsuario().getNombres() + " " + v.getUsuario().getApellidos(), texto);
                celdaTexto(r, 4, v.getMetodoPago().toString(), texto);
                celdaNumero(r, 5, v.getTotalUnidades(), numero);
                celdaTexto(r, 6, "$" + formatearMiles(v.getCostoTotal()), numero);
                parImpar = !parImpar;
            }

            hoja.createFreezePane(0, filaEncabezadoIdx + 1);
            if (fila > filaEncabezadoIdx + 1) {
                hoja.setAutoFilter(new CellRangeAddress(filaEncabezadoIdx, filaEncabezadoIdx, 0, COLUMNAS.length - 1));
            }

            int[] anchos = {2200, 3200, 2600, 6500, 4200, 3200, 3800};
            for (int i = 0; i < COLUMNAS.length; i++) {
                hoja.setColumnWidth(i, anchos[i]);
            }

            libro.write(salida);
            return salida.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("No se pudo generar el Excel del reporte de ventas", e);
        }
    }

    private XSSFCellStyle estiloFilaDatos(XSSFWorkbook libro, boolean rosa, boolean alinearDerecha) {
        XSSFCellStyle estilo = libro.createCellStyle();
        if (rosa) {
            estilo.setFillForegroundColor(new XSSFColor(ROSA_CLARO_RGB, null));
            estilo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        }
        estilo.setBorderBottom(BorderStyle.HAIR);
        estilo.setBottomBorderColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estilo.setAlignment(alinearDerecha ? HorizontalAlignment.RIGHT : HorizontalAlignment.LEFT);
        XSSFFont fuente = libro.createFont();
        fuente.setColor(new XSSFColor(OSCURO_RGB, null));
        fuente.setFontHeightInPoints((short) 10);
        estilo.setFont(fuente);
        return estilo;
    }

    private void celdaTexto(Row fila, int columna, String valor, XSSFCellStyle estilo) {
        fila.createCell(columna).setCellValue(valor != null ? valor : "-");
        fila.getCell(columna).setCellStyle(estilo);
    }

    private void celdaNumero(Row fila, int columna, Number valor, XSSFCellStyle estilo) {
        fila.createCell(columna).setCellValue(valor != null ? valor.doubleValue() : 0);
        fila.getCell(columna).setCellStyle(estilo);
    }

    private void agregarCelda(PdfPTable tabla, String texto, Font fuente, int alineacion, Color fondo) {
        PdfPCell celda = new PdfPCell(new Phrase(texto != null ? texto : "-", fuente));
        celda.setPadding(6);
        celda.setBorder(Rectangle.BOTTOM);
        celda.setBorderColor(GRIS_BORDE_AWT);
        celda.setBorderWidth(0.5f);
        celda.setBackgroundColor(fondo);
        celda.setHorizontalAlignment(alineacion);
        tabla.addCell(celda);
    }

    private String formatearMiles(long valor) {
        return String.format("%,d", valor).replace(",", ".");
    }

    /**
     * Dibuja el encabezado rosa con el titulo en cada pagina y el pie con
     * el numero de pagina, para que el PDF se vea consistente al imprimir
     * varias hojas.
     */
    private static class EncabezadoPiePdf extends PdfPageEventHelper {
        private final String subtitulo;
        private final Font fuenteMarca = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 16, Color.WHITE);
        private final Font fuenteSubtitulo = FontFactory.getFont(FontFactory.HELVETICA, 10, Color.WHITE);
        private final Font fuentePie = FontFactory.getFont(FontFactory.HELVETICA, 8, Color.GRAY);

        EncabezadoPiePdf(String subtitulo) {
            this.subtitulo = subtitulo;
        }

        @Override
        public void onEndPage(PdfWriter writer, Document documento) {
            PdfPTable banda = new PdfPTable(2);
            try {
                banda.setTotalWidth(documento.getPageSize().getWidth() - documento.leftMargin() - documento.rightMargin());
                banda.setWidths(new float[]{2f, 1f});

                PdfPCell izquierda = new PdfPCell();
                izquierda.setBackgroundColor(OSCURO_AWT);
                izquierda.setBorder(0);
                izquierda.setPadding(12);
                izquierda.addElement(new Paragraph("FashFind", fuenteMarca));
                izquierda.addElement(new Paragraph(subtitulo, fuenteSubtitulo));
                banda.addCell(izquierda);

                PdfPCell derecha = new PdfPCell();
                derecha.setBackgroundColor(OSCURO_AWT);
                derecha.setBorder(0);
                derecha.setPadding(12);
                derecha.setHorizontalAlignment(Element.ALIGN_RIGHT);
                derecha.setVerticalAlignment(Element.ALIGN_MIDDLE);
                Paragraph fecha = new Paragraph(
                        "Generado el " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                        fuenteSubtitulo);
                fecha.setAlignment(Element.ALIGN_RIGHT);
                derecha.addElement(fecha);
                banda.addCell(derecha);

                banda.writeSelectedRows(0, -1, documento.leftMargin(),
                        documento.getPageSize().getHeight() - 20, writer.getDirectContent());
            } catch (Exception e) {
                throw new RuntimeException("No se pudo dibujar el encabezado del PDF", e);
            }

            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_RIGHT,
                    new Phrase("Pagina " + documento.getPageNumber(), fuentePie),
                    documento.getPageSize().getWidth() - documento.rightMargin(),
                    documento.bottomMargin() - 12, 0);
            ColumnText.showTextAligned(writer.getDirectContent(), Element.ALIGN_LEFT,
                    new Phrase("FashFind - Sistema de gestion", fuentePie),
                    documento.leftMargin(),
                    documento.bottomMargin() - 12, 0);
        }
    }
}