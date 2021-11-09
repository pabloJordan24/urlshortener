$(document).ready(

    function() {

        $('#shortener :checkbox').change(function() {
             // this will contain a reference to the checkbox
             //alert("hola")
             //if (this.checked) {
                 // the checkbox is now checked

                  console.log(this.checked)
                    var envio = "";
                    if (this.checked){
                        envio = "true"
                    }else{
                        envio = "false"
                    }
                    $.ajax({
                        type : "POST",
                        url : "/api/checkqr",
                        //async: false,
                        contentType: 'application/x-www-form-urlencoded',

                        //dataType: "json",
                        data : {isCheck: envio},

                    });


             /*} else {
                 // the checkbox is now no longer checked
                 console.log("FALSE")
             }*/
        });
        $("#shortener").submit(
            function(event) {
                event.preventDefault();
                $.ajax({
                    type : "POST",
                    url : "/api/link",
                    data : $(this).serialize(),

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
                              $('#qruri').show();
                               $("#qruri").html(
                                "<input id='usertext' type='text' class='center-block form-control input-lg'"
                                +" title= "
                                + msg.qr
                                +" value= "
                                +msg.qr
                                +" name='shortUrlBox' readonly><span class='input-group-btn'><button class='btn btn-lg btn-primary' type='submit'>Info!</button></span>"
                                /*"<p>QR uri</p><div class='alert alert-success lead'><a id='qrredirect' target='_blank' href='/qr' role='button'>"
                                 + msg.qr
                                 +"</a></div>"*/
                                 );

                        }else{
                            $('#qruri').hide();

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

        $("#imageQR").submit(
        function(event) {
            event.preventDefault();
            //alert($(this).serialize().split("2F").pop())

            $.ajax({
                type : "GET",
                url : $(this).serialize().split("2F").pop(),
                success : function(response) {

                   // $("#resultInfo").html(
                        console.log(response)
                        $('#resultInfo').show();
                        $('#resultInfo').attr('src', `data:image/png;base64,${response.qr}`);

                    //);
                },
                error : function() {
                     $("#resultInfo").html(
                        "<div class='alert alert-danger lead'>ERROR</div>");
                }
            });
        });



    });