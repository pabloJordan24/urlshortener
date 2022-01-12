$(document).ready(
    function () {
        /*$("#shortener").submit(
            function(event) {
                event.preventDefault();
                //alert($(this).serialize())
                $.ajax({
                    type : "POST",
                    url : "/api/link",
                    data : $(this).serialize(),
                    success : function(msg, status, request) {
                        $("#result").html(
                            "<div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                    },
                    error : function() {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            }
        );*/
  
        // mostramos la info de la tiny url.
        $("#infoShortUrl").submit(
        function(event) {
            event.preventDefault();
            //alert($(this).serialize().split("tiny-").pop())
            $.ajax({
                type : "GET",
                url : "/"+$(this).serialize().split("tiny-").pop()+".json",
                success : function(response) {
                    $("#resultInfo").html(
                        "<div class='alert alert-info lead'>"
                        + "<p>" + "Total number of clicks: " + response.numClicks +"</p>"
                        + "<p>" + "Date of creation: " + response.creationDate +"</p>"
                        + "<p>" + "Target URL: " + response.uriDestino +"</p>"
                        + "<p>" + "Users in the last 7 days: " + response.usersClicks + "</p>"
                        + "</div>"
                    );
                },
                error : function() {
                     $("#resultInfo").html(
                        "<div class='alert alert-danger lead'>ERROR</div>");
                }
            });
        });

        // hasta que no mostremos el QR, esta opción estará oculta.
        $('#imageQR').hide();
        
        // hacemos todo el proceso de acortar la url (mirar si el checkbox está checkeado).
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                var checked=false
                if ($("#shortener :checkbox").is(":checked")) {
                    checked=true
                }
                var uridec = decodeURIComponent($(this).serialize().split("=").pop())
                console.log(uridec)
                
                $.ajax({
                    type : "POST",
                    url : "/api/link",
                    data : {"url":uridec, "qr":checked},

                    success : function(msg, status, request) {
                        $("#result").html(
                            "<p>ShortUrl</p><div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");

                        if( $("#qrCheck").is(":checked") ) {
                              $('#imageQR').show();
                              $('#qrurlbuena').attr('href', msg.qr);
                              $("#qrurlbuena").text(msg.qr);
                        }else{
                            $('#imageQR').hide();
                        }
                    },

                    error : function() {
                        $("#result").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                        $("#qruri").html(
                                "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });

        //Para cargar la imagen del qr al pulsar sobre su url
        $('#qrurlbuena').click(
        function(event) {
            event.preventDefault();
            var ref = $("#qrurlbuena" ).prop( "href")
        
            $.ajax({
                type : "GET",
                url : "/qrcode-" + $("#qrurlbuena" ).prop("href").split("qrcode-").pop(),
                success : function(response) {
                    window.open(
                        ref,
                        '_blank' // <- This is what makes it open in a new window.
                    );
                },
                error : function() {
                    window.open(
                        ref,
                        '_blank' // <- This is what makes it open in a new window.
                    );
                }
            });
        });

        function download(filename, text) {
            var element = document.createElement('a');
            element.setAttribute('href', 'data:text/csv;charset=utf-8,' + encodeURIComponent(text));
            element.setAttribute('download', filename);

            element.style.display = 'none';
            document.body.appendChild(element);

            element.click();

            document.body.removeChild(element);
        }

        const input = document.querySelector('input[type="file"]')
        var lines
        input.addEventListener('change', function (e) {
            console.log(input.files)
            console.log(input.files[0].name)
            const reader = new FileReader()
            reader.onload = function () {
                console.log(reader.result)
                lines = reader.result.split('\n')
                console.log(lines)
            }
            reader.readAsText(input.files[0])
        }, false)

        $("#shortenerCSV").submit(
            function(event) {
                event.preventDefault();
                $.ajax({
                    url: "/csv",
                    type: "POST",
                    data: new FormData(this),
                    enctype: 'multipart/form-data',
                    processData: false,
                    contentType: false,
                    cache: false,
                    success: function (res) {
                        console.log(res);
                        var host = "ws://localhost:8080/csv/progress/" +  res;
                        var wSocket = new WebSocket(host);
                        var browserSupport = ("WebSocket" in window) ? true : false;
                        var ipIterator = 0;
                        var ipResult = "";
                        var htmlList = "";

                        function initializeReception()
                        {
                            if (browserSupport)
                            {
                                wSocket.onopen = function ()
                                {

                                };
                            } else
                            {
                                // No hay soporte, posiblemente un navegador obsoleto
                                alert("WebSocket no es soportado en su browser. Utilice uno moderno.");
                            }
                        };

                        wSocket.onmessage = onMessage;

                         function onMessage(evt) {
                            var received_msg = evt.data;
                            console.log(received_msg);
                            if (received_msg == "Send me the URLs") {
                                $("#resultCSV").html("<label></label>");
                                if(lines.length == ipIterator) {
                                    addMsg("There are no more URLs");
                                    download("shortened_" + input.files[0].name, ipResult);
                                } else {
                                    addMsg(lines[ipIterator]);
                                    ipIterator++;
                                }
                            } else {
                                if (received_msg[0] == ",") {
                                    ipResult = ipResult + "\n";
                                } else {
                                    ipResult = ipResult + received_msg + "\n";
                                }
                                var porcentaje = (ipIterator)*(100/lines.length)
                                var urlacortada = received_msg.split(',');
                                if (urlacortada[1][0] == "[") {
                                    if (urlacortada[1][1] != "]") {
                                        htmlList = htmlList + "<div class='alert alert-danger lead'>" + urlacortada[1] + "</div>";
                                    }
                                } else {
                                    htmlList = htmlList + "<div class='alert alert-success lead'><div><a target='_blank' href='" + urlacortada[0] + "'>" + urlacortada[0] + "</a></div><a target='_blank' href='" + urlacortada[1] + "'>" + urlacortada[1] + "</a></div>";
                                }
                                if(lines.length == ipIterator) {
                                    $("#resultCSV").html(
                                        "<label for=file>Shortening progress:</label><progress id=file max=100 value=100> 100% </progress>" +
                                        "<div style='overflow-y:auto; height:300px;'>" + htmlList + "</div>");
                                    addMsg("There are no more URLs");
                                    download("shortened_" + input.files[0].name, ipResult);
                                } else {
                                    $("#resultCSV").html(
                                        "<label for=file>Shortening progress:</label><progress id=file max=100 value=" + porcentaje + ">" + porcentaje + "% </progress>" +
                                        "<div style='overflow-y:auto; height:300px;'>" + htmlList + "</div>");
                                    addMsg(lines[ipIterator]);
                                    ipIterator++;
                                }
                            }
                        };

                        function addMsg(message) {
                            wSocket.send(message);
                        };
                    },
                    error: function (err) {
                        console.error(err);
                        $("#resultCSV").html(
                            "<div class='alert alert-danger lead'>ERROR</div>");
                    }
                });
            });
    }
);