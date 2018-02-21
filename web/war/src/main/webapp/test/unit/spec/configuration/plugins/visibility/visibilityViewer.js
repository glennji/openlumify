define([
    'react',
    'react-dom',
    'components/visibility/default/VisibilityViewer'
], function(
    React,
    ReactDOM,
    DefaultVisibilityViewer) {

    describe('Visibility Viewer', function () {

        beforeEach(function () {
            this.node = document.createElement('div');
        });

        it('Should populate the node with value', function () {
            const viewer = React.createElement(DefaultVisibilityViewer, { value: 'a' });
            ReactDOM.render(viewer, this.node);
            const componentNode = this.node.children[0];

            expect(componentNode.textContent).to.equal('a');
        });

        it('Should be public if no value specified', function () {
            const viewer = React.createElement(DefaultVisibilityViewer, {});
            ReactDOM.render(viewer, this.node);
            const componentNode = this.node.children[0];

            expect(componentNode.textContent).to.equal('visibility.blank');
        })

        it('Should still display falsy value', function () {
            let viewer, componentNode;

            viewer = React.createElement(DefaultVisibilityViewer, { value: '0' });
            ReactDOM.render(viewer, this.node);
            componentNode = this.node.children[0];

            expect(componentNode.textContent).to.equal('0');

            viewer = React.createElement(DefaultVisibilityViewer, { value: 0 });
            ReactDOM.render(viewer, this.node);
            componentNode = this.node.children[0];

            expect(componentNode.textContent).to.equal('0');
        })
    })
});
