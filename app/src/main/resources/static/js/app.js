$(document).ready(

    function() {

        $('#imageQR').hide();
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
                    data : {"url":uridec,"qr":checked},
                    success : function(msg, status, request) {

                        console.log(request)
                        console.log("Se envia???")
                        console.log(request.getResponseHeader('qr'))
                        $("#result").html(

                            "<p>ShortUrl</p><div class='alert alert-success lead'><a target='_blank' href='"
                            + request.getResponseHeader('Location')
                            + "'>"
                            + request.getResponseHeader('Location')
                            + "</a></div>");
                        console.log($("#qrCheck").is(":checked"))
                        if( $("#qrCheck").is(":checked") ) {
                              console.log("Activadooo")
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
           // alert("/qrcode-" + $("#qrurlbuena" ).prop( "href").split("qrcode-").pop())
            event.preventDefault();
            //alert($("#qrurlbuena" ).prop( "href").split("qrcode-").pop())
            var ref = $("#qrurlbuena" ).prop( "href")
            $.ajax({
                type : "GET",
                url : "/qrcode-" + $("#qrurlbuena" ).prop( "href").split("qrcode-").pop(),
                success : function(response) {
                     window.open(
                        ref,
                        '_blank' // <- This is what makes it open in a new window.
                     );
                },
                error : function() {
                  // alert("errooooor")
                }

            });

        });



    });