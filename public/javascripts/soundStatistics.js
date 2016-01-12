/**
 * Created by dmitry on 11.12.15.
 */

var REDRAW_DELAY = 100*1000; //Chart redraw interval

function populateCameraSelect(){
    var select = $('#select_cam');
    $.getJSON('/cameras', function (data) {
        $.each(data, function(i, cam){
            select.append('<option value=' + cam.id + '>' + cam.name + '</option>');
        });
    });
}

$( document ).ready(function() {
    $.ajaxSetup({ cache: false });
    Highcharts.setOptions({
        global: {
            useUTC: true
        }
    });

    populateCameraSelect();
    $('#select_cam').on('change', function(){
        drawStatistics();
    });
   // window.setInterval(drawStatistics, REDRAW_DELAY);
});

function drawStatistics(){

    var $selectedCam = $('#select_cam').val();
    if(!$selectedCam)return;

    $.getJSON('/sounds/' + $selectedCam, function (data) {

        var seriesData = [];
        for (var i = 0; i < data.length; i++){
            var x = new Date(data[i].startDate).getTime();
            var y = data[i].duration/1000;
            seriesData.push([x, y]);
        }

        $('#container').highcharts({
            chart: {
                zoomType: 'x'
            },
            title: {
                text: 'Sound Activity rate over time'
            },
            subtitle: {
                text: document.ontouchstart === undefined ?
                    'Click and drag in the plot area to zoom in' : 'Pinch the chart to zoom in'
            },
            xAxis: {
                type: 'datetime'
            },
            yAxis: {
                title: {
                    text: 'Sound Activity'
                }
            },
            legend: {
                enabled: false
            },
            plotOptions: {
                area: {
                    fillColor: {
                        linearGradient: {
                            x1: 0,
                            y1: 0,
                            x2: 0,
                            y2: 1
                        },
                        stops: [
                            [0, Highcharts.getOptions().colors[0]],
                            [1, Highcharts.Color(Highcharts.getOptions().colors[0]).setOpacity(0).get('rgba')]
                        ]
                    },
                    marker: {
                        radius: 2
                    },
                    lineWidth: 1,
                    states: {
                        hover: {
                            lineWidth: 1
                        }
                    },
                    threshold: null
                }
            },

            series: [{
                type: 'area',
                name: 'Sound Activity',
                data: seriesData
            }]
        });
    });
}