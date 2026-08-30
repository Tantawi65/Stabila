import tflite

def print_shape(shape):
    return [shape[i] for i in range(shape.size)]

with open('e:/University Programs/Stabila/feature/camera/src/main/assets/zero_dce.tflite', 'rb') as f:
    buf = f.read()
    model = tflite.Model.GetRootAsModel(buf, 0)
    subgraph = model.Subgraphs(0)
    
    print('INPUTS:')
    for i in range(subgraph.InputsLength()):
        tensor = subgraph.Tensors(subgraph.Inputs(i))
        print(tensor.Name().decode('utf-8'), tensor.ShapeAsNumpy())
        
    print('OUTPUTS:')
    for i in range(subgraph.OutputsLength()):
        tensor = subgraph.Tensors(subgraph.Outputs(i))
        print(tensor.Name().decode('utf-8'), tensor.ShapeAsNumpy())
