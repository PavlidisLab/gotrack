function showTerminal() {
    PF('terminalDialogWdg').show();
    PF('terminalWdg').focus();
}
function onLoad() {
}
$(document).ready(function () {
    $( document ).tooltip({
        items: ".help-icon[title-id],.help-icon[title]",
        content: function () {
            var title_id = $(this).attr('title-id');
            if (title_id) {
                return $(title_id).clone();
            } else {
                return $(this).attr('title')
            }
        },
        position: { my: "left top+5", at: "left bottom", collision: "flipfit" },
        show: false,
        hide: false,
        open: function(event, ui) {
            // Click to keep open functionality
            var $this = $.data(this, 'ui-tooltip');
            var tooltipData = $this.tooltips[ui.tooltip[0].id];
            tooltipData.element.off('click.pin').on('click.pin', function(e) {
                var $elem = $(this);
                tooltipData.closing = !tooltipData.pinned;
                tooltipData.pinned = !tooltipData.pinned;
                $.each( $this.tooltips, function( id, ttd ) {
                    console.log(id, ttd, ttd.element[0], $elem[0], ttd.element[0] === $elem[0]);
                    if (ttd.pinned && ttd.element[0] !== $elem[0]) {
                        var event = $.Event("blur");
                        event.target = event.currentTarget = ttd.element[0];
                        ttd.closing=false;
                        ttd.pinned=false;
                        $this.close(event, true);
                    }
                } );
                e.stopPropagation();
            });
        },
        close: function(evt, ui) {
            // Only get rid of extraneous elements. We'll keep the one that was created last.
            // See https://bugs.jqueryui.com/ticket/10689
            $.data(this, "ui-tooltip").liveRegion.children(":not(:last)").remove();
        }
    });
});