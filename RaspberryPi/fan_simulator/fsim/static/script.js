$(document).ready(function(){

    $("#btn_on").click(function(){ $.get("/fan/on"); });
    $("#btn_off").click(function(){ $.get("/fan/off"); });

    //$("#btn_speed1").click(function(){ $.get("/fan/speed/10"); });
    //$("#btn_speed2").click(function(){ $.get("/fan/speed/100"); });

    function change_temperature(t) {
        $.get("/temp/" + t);
        $.get("/fan/speed/" + (t*10));
    }

    $("#btn_temp0").click(function(){ change_temperature(0); });
    $("#btn_temp1").click(function(){ change_temperature(1); });
    $("#btn_temp2").click(function(){ change_temperature(2); });
    $("#btn_temp3").click(function(){ change_temperature(3); });
    $("#btn_temp4").click(function(){ change_temperature(4); });
    $("#btn_temp5").click(function(){ change_temperature(5); });
    $("#btn_temp6").click(function(){ change_temperature(6); });
    $("#btn_temp7").click(function(){ change_temperature(7); });
    $("#btn_temp8").click(function(){ change_temperature(8); });
    $("#btn_temp9").click(function(){ change_temperature(9); });
    $("#btn_temp10").click(function(){ change_temperature(10); });
    $("#btn_temp11").click(function(){ change_temperature(11); });
    $("#btn_temp12").click(function(){ change_temperature(12); });
    $("#btn_temp13").click(function(){ change_temperature(13); });
    $("#btn_temp14").click(function(){ change_temperature(14); });
    $("#btn_temp15").click(function(){ change_temperature(15); });
}); 
