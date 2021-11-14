$(document).ready(
    function () {
        $("#shortener").submit(
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
        );
  
        $("#infoShortUrl").submit(
        function(event) {
            event.preventDefault();
            //alert($(this).serialize().split("tiny-").pop())
            $.ajax({
                type : "GET",
                url : "/info/"+$(this).serialize().split("tiny-").pop(),
                success : function(response) {
                    $("#resultInfo").html(
                        "<div class='alert alert-info lead'>"
                        + "<p>" + "Total number of clicks: " + response.numClicks +"</p>"
                        + "<p>" + "Date of creation: " + response.creationDate +"</p>"
                        + "<p>" + "Target URL: " + response.uriDestino +"</p>"
                        + "</div>"
                    );
                },
                error : function() {
                     $("#resultInfo").html(
                        "<div class='alert alert-danger lead'>ERROR</div>");
                }
            });
        });

        /*$("#shortenerCSV").submit(
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
                        //window.open("http://localhost:8080/csv/download")
                    },
                    error: function (err) {
                        console.error(err);
                    }
                });
            });*/
    }
    
);