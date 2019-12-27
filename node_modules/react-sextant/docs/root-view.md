# RootView

root view根视图
顾名思义从根组件中插入视图，不受当前页面影响，效果与`Modal`,`Toast`相似。

## Import
```jsx harmony
import {RootView} from 'react-sextant'
//or
import InputItem from 'react-sextant/lib/root-view'
```
## Example
Basic usage
```jsx harmony
RootView.setView(
    <View style={{flex:1,backgroundColor:'#000000',opacity:0.5}}>
        <Text style={{color:'#FFFFFF'}}>Modal Text</Text>
    </View>
)
```
## API

| void | description |
|----|----|
|addView(Element)|Add root view|
|hide()|Hidden root view|
