package io.github.gaming32.qrcodepaster

import com.google.zxing.BinaryBitmap
import com.google.zxing.MultiFormatReader
import com.google.zxing.NotFoundException
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer
import java.awt.Color
import java.awt.Desktop
import java.awt.Image
import java.awt.Toolkit
import java.awt.datatransfer.DataFlavor
import java.awt.event.ActionEvent
import java.awt.image.BufferedImage
import java.io.IOException
import java.net.URI
import java.net.URISyntaxException
import javax.imageio.ImageIO
import javax.swing.*
import kotlin.concurrent.thread

val codeReader = MultiFormatReader()

fun main() {
    val defaultImage = BufferedImage(500, 500, BufferedImage.TYPE_4BYTE_ABGR)
    defaultImage.graphics.let { g ->
        g.color = Color.BLACK
        g.font = g.font.deriveFont(36f)
        val text = "Paste a QR code!"
        g.drawString(
            text,
            250 - g.fontMetrics.stringWidth(text) / 2,
            250 - g.fontMetrics.height / 2
        )
        g.dispose()
    }

    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName())

    SwingUtilities.invokeLater {
        val frame = JFrame("QR Code Paster")
        frame.iconImages = listOf(
            ImageIO.read(object {}.javaClass.getResource("/icon_16.png")),
            ImageIO.read(object {}.javaClass.getResource("/icon_32.png")),
        )

        val imageLabel = JLabel(ImageIcon(defaultImage))

        val outputText = JTextField()
        outputText.isEditable = false

        val openAsLink = JButton("Open as Link")
        openAsLink.isEnabled = false
        openAsLink.addActionListener {
            thread(isDaemon = true, name = "Open-Link") {
                try {
                    Desktop.getDesktop().browse(URI(outputText.text))
                } catch (e: IOException) {
                    e.printStackTrace()
                    SwingUtilities.invokeLater {
                        JOptionPane.showMessageDialog(
                            frame,
                            "Invalid link!\n${e.message}",
                            frame.title,
                            JOptionPane.ERROR_MESSAGE
                        )
                    }
                }
            }
            outputText.requestFocus()
        }

        outputText.actionMap.put("paste-from-clipboard", object : AbstractAction() {
            override fun actionPerformed(e: ActionEvent) {
                val clipboard = Toolkit.getDefaultToolkit().systemClipboard
                if (!clipboard.isDataFlavorAvailable(DataFlavor.imageFlavor)) {
                    return
                }
                val image = clipboard.getData(DataFlavor.imageFlavor) as BufferedImage
                imageLabel.icon = ImageIcon(image.getScaledInstance(
                    defaultImage.width, defaultImage.height, Image.SCALE_SMOOTH
                ))
                outputText.text = "Processing..."
                thread(isDaemon = true, name = "QR-Code-Processor") {
                    val source = BufferedImageLuminanceSource(image)
                    val bitmap = BinaryBitmap(HybridBinarizer(source))

                    val result = try {
                        codeReader.decode(bitmap)
                    } catch (e: NotFoundException) {
                        null
                    }
                    SwingUtilities.invokeLater {
                        outputText.text = result?.text ?: "No QR code found!"
                        result?.text?.let {
                            openAsLink.isEnabled = try {
                                URI(it)
                                true
                            } catch (_: URISyntaxException) {
                                false
                            }
                        }
                    }
                }
            }
        })
        outputText.requestFocus()

        val layout = GroupLayout(frame.contentPane)
        frame.contentPane.layout = layout
        layout.setHorizontalGroup(layout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addComponent(imageLabel)
            .addGroup(layout.createSequentialGroup()
                .addComponent(outputText)
                .addComponent(openAsLink)
            )
        )
        layout.setVerticalGroup(layout.createSequentialGroup()
            .addComponent(imageLabel)
            .addGroup(layout.createParallelGroup()
                .addComponent(outputText)
                .addComponent(openAsLink)
            )
        )

        frame.pack()
        frame.isResizable = false
        frame.defaultCloseOperation = JFrame.DISPOSE_ON_CLOSE
        frame.isVisible = true
    }
}
