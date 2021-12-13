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
                url : "/"+$(this).serialize().split("tiny-").pop()+".json",
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
    }
);